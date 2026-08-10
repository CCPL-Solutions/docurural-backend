#!/usr/bin/env python3
"""Mueve el issue padre de la release en GitHub Projects v2 segun el pipeline CI/CD.

Lee `release-configuration.json` para saber cual es el issue padre y la version
en curso, y usa la API GraphQL de GitHub para mover el campo "Status" del
Project del estado FROM_STATUS al estado TO_STATUS.

Disenado para nunca tumbar un despliegue: cualquier condicion que no permita
completar la transicion (config invalida, version distinta, issue fuera del
proyecto, estado actual distinto al esperado, error de red o de la API)
termina en exit 0 con un mensaje de warning, nunca en una excepcion sin capturar.

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
    query = """
    query($owner: String!, $repo: String!, $number: Int!) {
      repository(owner: $owner, name: $repo) {
        issue(number: $number) {
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
            return item["id"], item["project"]["id"], current_status
    return None, None, None


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


def run() -> int:
    token = os.environ.get("GH_TOKEN")
    to_status = os.environ.get("TO_STATUS")
    from_status = os.environ.get("FROM_STATUS")
    deploy_version = os.environ.get("DEPLOY_VERSION")
    config_path = os.environ.get("CONFIG_PATH", "release-configuration.json")

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

    try:
        config = load_config(config_path)
        config_version = normalize_version(config["version"])
        owner, repo, issue_number = parse_issue_url(config["releaseIssueUrl"])
        project_number = config["project"]["number"]
    except (OSError, json.JSONDecodeError, KeyError, ValueError) as exc:
        warn(f"no se pudo leer '{config_path}': {exc} — no se actualiza el tablero.")
        return 0

    if config_version != normalize_version(deploy_version):
        warn(
            f"la version desplegada ({deploy_version}) no coincide con la version en "
            f"{config_path} ({config['version']}) — no se actualiza el tablero."
        )
        return 0

    try:
        item_id, project_id, current_status = find_project_item(
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
    return 0


if __name__ == "__main__":
    sys.exit(run())
