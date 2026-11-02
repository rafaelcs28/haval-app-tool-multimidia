# Onboarding

Este guia prepara uma nova sessão de desenvolvimento sem violar contratos, credenciais ou a política de build do projeto.

## Pré-requisitos

- Git;
- Java 17 para inspeção e ferramentas, sem Gradle local;
- Node.js 22 e npm para temas e checks portáteis;
- Python 3.11 para suítes documentadas;
- acesso à fila autorizada do Mac Mini quando houver build Android.

Credenciais, arquivos `.env`, material de assinatura, logs privados e configurações de serviço não pertencem ao repositório.

## Primeira leitura

1. Execute `git status --short` e `git rev-parse --short HEAD`.
2. Leia `AGENTS.md`.
3. Leia o início de `.ai-context/HANDOFF.md`, `KNOWN-ISSUES.md` e `NEXT-STEPS.md`.
4. Leia `docs/architecture/overview.md` e o contrato específico do fluxo alterado.
5. Confirme no código e nos artefatos atuais qualquer afirmação recebida por handoff.

## Validação local permitida

Checks Node, Python e nativos devem seguir `docs/quality.md`. Para um tema alterado, instale dependências com lockfile e execute os scripts daquele tema. Não execute `./gradlew` no coordenador.

## Build Android

Depois de revisar e criar um commit imutável, use:

```bash
scripts/build-on-mac-mini.sh <COMMIT>
```

Consulte `docs/BUILD_ANDROID_ON_MAC_MINI.md` para fila, artefatos e checksums. Se a fila estiver indisponível, pare e reporte; não faça fallback local.

## Veículo e serviços externos

Ferramentas em `tools/headunit-dev/` e integrações externas só podem ser usadas dentro da autorização explícita vigente. Uma tarefa de código ou documentação não autoriza ligar o veículo, instalar APK, reiniciar a central, publicar release ou alterar PR.
