## Descripción

<!-- Qué cambia y por qué. -->

## Tipo de cambio

- [ ] Feature (`feature/*` → `develop`)
- [ ] Fix de certificación (`bugfix/*` → `release/x.y.z`)
- [ ] Hotfix de producción (`hotfix/*` → `main`)
- [ ] Corte de release / otro

## Rama destino

- [ ] Confirmo que la rama destino de este PR es la correcta según la [estrategia de ramas](../docs/ci-cd.md).

## Ticket / QA

<!-- ID del ticket (ej. QA-123) si aplica. -->

## Checklist

- [ ] `./mvnw clean verify` pasa localmente (tests + cobertura JaCoCo).
- [ ] Agregué o actualicé pruebas para el cambio.
- [ ] Si este PR va a `release/*` o `hotfix/*`: el fix también debe llegar a `develop` (cherry-pick inmediato o vía el back-merge automático al cerrar la release).
- [ ] No hay strings hardcodeados de negocio (se usó `MessageResolver` / `messages.properties`).
