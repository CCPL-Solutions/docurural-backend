#!/usr/bin/env python3
"""Mueve el issue padre de la release en GitHub Projects v2 segun el pipeline CI/CD,
y propaga ese estado a sus sub-issues (las HU tecnicas que entran con la release).

Lee `release-configuration.json` para saber cual es el issue padre y la version
en curso, y usa la API GraphQL de GitHub para mover el campo "Status" del
Project. Lo invocan los workflows de despliegue (cd-dev.yml, cd-qa.yml,
cd-prod.yml) en cada punto determinista del pipeline.

Disenado para nunca tumbar un despliegue: cualquier condicion que no permita
completar una transicion (config invalida, version distinta, issue fuera del
proyecto, estado actual distinto al esperado, error de red o de la API)
termina en exit 0 con un mensaje de warning, nunca en una excepcion sin capturar.

La cascada cuando alguien mueve la tarjeta del padre a mano (en vez de via
pipeline) la cubre un workflow aparte en el repo project-automation, para no
ensuciar las ejecuciones de Actions de este repo — ver docs/ci-cd.md, seccion
6.1.

Variables de entorno:
    GH_TOKEN          PAT con scope 'project' (requerido)
    TO_STATUS         Nombre exacto del estado destino (requerido)
    FROM_STATUS       Nombre exacto del estado origen esperado (requerido)
    DEPLOY_VERSION    Version que se esta desplegando, ej. "1.0.0-rc.3" (requerido)
    CONFIG_PATH       Ruta al JSON de configuracion (default: release-configuration.json)
"""

import json
import os
import re
import sys
import urllib.error
import urllib.request

GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"
STATUS_FIELD_NAME = "Status"
SUB_ISSUES_LIMIT = 100
ISSUE_URL_PATTERN = re.compile(
    r"^https://github\.com/(?P<owner>[^/]+)/(?P<repo>[^/]+)/issues/(?P<number>\d+)/?$"
)


def warn(message: str) -> None:
    print(f"::warning::[update_project_status] {message}")


def info(message: str) -> None:
    print(f"[update_project_status] {message}")


def normalize_version(version: str) -> str:
    return version.split("-", 1)[0].lstrip("v")


def graphql_request(token: str, query: str, variables: dict) -> dict:
    payload = json.dumps({"query": query, "variables": variables}).encode("utf-8")
    request = urllib.request.Request(
        GITHUB_GRAPHQL_URL,
        data=payload,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "User-Agent": "docurural-backend-release-automation",
        },
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        body = json.loads(response.read().decode("utf-8"))
    if body.get("errors"):
        raise RuntimeError(f"GraphQL respondio con errores: {body['errors']}")
    return body["data"]


def load_config(config_path: str) -> dict:
    with open(config_path, encoding="utf-8") as config_file:
        return json.load(config_file)


def parse_issue_url(issue_url: str) -> tuple[str, str, int]:
    match = ISSUE_URL_PATTERN.match(issue_url.strip())
    if not match:
        raise ValueError(f"releaseIssueUrl con formato invalido: {issue_url!r}")
    return match.group("owner"), match.group("repo"), int(match.group("number"))


def find_project_item(token: str, owner: str, repo: str, issue_number: int, project_number: int):
    """Devuelve (item_id, project_id, current_status, issue_node_id) del issue en el
    Project indicado, o (None, None, None, issue_node_id) si no esta en ese Project."""
    query = """
    query($owner: String!, $repo: String!, $number: Int!) {
      repository(owner: $owner, name: $repo) {
        issue(number: $number) {
          id
          projectItems(first: 20) {
            nodes {
              id
              project { id number }
              fieldValueByName(name: "Status") {
                ... on ProjectV2ItemFieldSingleSelectValue { name }
              }
            }
          }
        }
      }
    }
    """
    data = graphql_request(token, query, {"owner": owner, "repo": repo, "number": issue_number})
    issue = data.get("repository", {}).get("issue")
    if issue is None:
        raise ValueError(f"No se encontro el issue #{issue_number} en {owner}/{repo}")

    for item in issue["projectItems"]["nodes"]:
        if item["project"]["number"] == project_number:
            current_status = (item.get("fieldValueByName") or {}).get("name")
            return item["id"], item["project"]["id"], current_status, issue["id"]
    return None, None, None, issue["id"]


def find_sub_issues(token: str, owner: str, repo: str, issue_number: int, project_number: int) -> list[dict]:
    """Devuelve los sub-issues del padre que estan en el Project indicado, con su
    item_id y estado actual. Los que no estan en el Project se registran como warning
    y se excluyen de la lista."""
    query = """
    query($owner: String!, $repo: String!, $number: Int!, $limit: Int!) {
      repository(owner: $owner, name: $repo) {
        issue(number: $number) {
          subIssues(first: $limit) {
            totalCount
            nodes {
              number
              repository { nameWithOwner }
              projectItems(first: 20) {
                nodes {
                  id
                  project { number }
                  fieldValueByName(name: "Status") {
                    ... on ProjectV2ItemFieldSingleSelectValue { name }
                  }
                }
              }
            }
          }
        }
      }
    }
    """
    data = graphql_request(
        token,
        query,
        {"owner": owner, "repo": repo, "number": issue_number, "limit": SUB_ISSUES_LIMIT},
    )
    sub_issues = data["repository"]["issue"]["subIssues"]
    if sub_issues["totalCount"] > SUB_ISSUES_LIMIT:
        warn(
            f"el issue padre tiene {sub_issues['totalCount']} sub-issues, solo se procesan "
            f"los primeros {SUB_ISSUES_LIMIT}."
        )

    children = []
    for node in sub_issues["nodes"]:
        label = f"{node['repository']['nameWithOwner']}#{node['number']}"
        item = next(
            (i for i in node["projectItems"]["nodes"] if i["project"]["number"] == project_number),
            None,
        )
        if item is None:
            warn(f"sub-issue {label} no esta en el Project #{project_number} — se omite.")
            continue
        current_status = (item.get("fieldValueByName") or {}).get("name")
        children.append({"label": label, "item_id": item["id"], "current_status": current_status})
    return children


def resolve_status_option(token: str, project_id: str, status_name: str) -> tuple[str, str]:
    query = """
    query($projectId: ID!) {
      node(id: $projectId) {
        ... on ProjectV2 {
          field(name: "Status") {
            ... on ProjectV2SingleSelectField {
              id
              options { id name }
            }
          }
        }
      }
    }
    """
    data = graphql_request(token, query, {"projectId": project_id})
    field = data["node"]["field"]
    for option in field["options"]:
        if option["name"] == status_name:
            return field["id"], option["id"]
    raise ValueError(f"El campo '{STATUS_FIELD_NAME}' no tiene una opcion llamada {status_name!r}")


def update_status(token: str, project_id: str, item_id: str, field_id: str, option_id: str) -> None:
    mutation = """
    mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
      updateProjectV2ItemFieldValue(
        input: {
          projectId: $projectId
          itemId: $itemId
          fieldId: $fieldId
          value: { singleSelectOptionId: $optionId }
        }
      ) {
        projectV2Item { id }
      }
    }
    """
    graphql_request(
        token,
        mutation,
        {"projectId": project_id, "itemId": item_id, "fieldId": field_id, "optionId": option_id},
    )


def cascade_to_children(token: str, project_id: str, children: list[dict], to_status: str) -> None:
    """Mueve a to_status cada hijo cuyo estado actual sea distinto. Un fallo en un
    hijo se reporta y no interrumpe a los demas."""
    if not children:
        info("el issue padre no tiene sub-issues en este Project — nada que propagar.")
        return

    try:
        field_id, option_id = resolve_status_option(token, project_id, to_status)
    except (urllib.error.URLError, RuntimeError, ValueError) as exc:
        warn(f"no se pudo resolver el estado '{to_status}' para propagar a los hijos: {exc}")
        return

    moved, already, failed = 0, 0, 0
    for child in children:
        if child["current_status"] == to_status:
            already += 1
            continue
        try:
            update_status(token, project_id, child["item_id"], field_id, option_id)
            moved += 1
            info(f"sub-issue {child['label']}: '{child['current_status']}' -> '{to_status}'")
        except (urllib.error.URLError, RuntimeError) as exc:
            failed += 1
            warn(f"no se pudo mover el sub-issue {child['label']} a '{to_status}': {exc}")

    info(f"cascada a '{to_status}': {moved} movidos, {already} ya estaban, {failed} fallidos.")


def load_release_config(config_path: str):
    """Devuelve (owner, repo, issue_number, project_number, config) o None si la
    configuracion no se pudo leer (ya emitio el warning correspondiente)."""
    try:
        config = load_config(config_path)
        owner, repo, issue_number = parse_issue_url(config["releaseIssueUrl"])
        project_number = config["project"]["number"]
        return owner, repo, issue_number, project_number, config
    except (OSError, json.JSONDecodeError, KeyError, ValueError) as exc:
        warn(f"no se pudo leer '{config_path}': {exc} — no se actualiza el tablero.")
        return None


def run_deploy(config_path: str) -> int:
    token = os.environ.get("GH_TOKEN")
    to_status = os.environ.get("TO_STATUS")
    from_status = os.environ.get("FROM_STATUS")
    deploy_version = os.environ.get("DEPLOY_VERSION")

    missing = [
        name
        for name, value in (
            ("GH_TOKEN", token),
            ("TO_STATUS", to_status),
            ("FROM_STATUS", from_status),
            ("DEPLOY_VERSION", deploy_version),
        )
        if not value
    ]
    if missing:
        warn(f"faltan variables de entorno requeridas: {', '.join(missing)} — no se actualiza el tablero.")
        return 0

    resolved = load_release_config(config_path)
    if resolved is None:
        return 0
    owner, repo, issue_number, project_number, config = resolved

    if normalize_version(config["version"]) != normalize_version(deploy_version):
        warn(
            f"la version desplegada ({deploy_version}) no coincide con la version en "
            f"{config_path} ({config['version']}) — no se actualiza el tablero."
        )
        return 0

    try:
        item_id, project_id, current_status, _issue_node_id = find_project_item(
            token, owner, repo, issue_number, project_number
        )
    except (urllib.error.URLError, RuntimeError, ValueError) as exc:
        warn(f"no se pudo consultar el issue padre: {exc} — no se actualiza el tablero.")
        return 0

    if item_id is None:
        warn(
            f"el issue {owner}/{repo}#{issue_number} no esta en el Project #{project_number} "
            "— no se actualiza el tablero."
        )
        return 0

    if current_status != from_status:
        info(
            f"estado actual del issue ('{current_status}') distinto al esperado ('{from_status}') "
            "— no se mueve la tarjeta (probablemente ya avanzo manualmente)."
        )
        return 0

    try:
        field_id, option_id = resolve_status_option(token, project_id, to_status)
        update_status(token, project_id, item_id, field_id, option_id)
    except (urllib.error.URLError, RuntimeError, ValueError) as exc:
        warn(f"no se pudo mover el issue a '{to_status}': {exc}")
        return 0

    info(f"issue {owner}/{repo}#{issue_number}: '{current_status}' -> '{to_status}'")

    try:
        children = find_sub_issues(token, owner, repo, issue_number, project_number)
        cascade_to_children(token, project_id, children, to_status)
    except (urllib.error.URLError, RuntimeError, ValueError) as exc:
        warn(f"no se pudieron consultar los sub-issues del padre: {exc}")

    return 0


def run() -> int:
    config_path = os.environ.get("CONFIG_PATH", "release-configuration.json")
    return run_deploy(config_path)


if __name__ == "__main__":
    sys.exit(run())
