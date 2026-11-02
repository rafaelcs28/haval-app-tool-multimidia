# Ferramentas e configuração de agentes

Este documento registra quais partes do toolkit foram adotadas, recusadas e validadas neste repositório.

## Origem da adoção

Configuração revisada a partir do `vibe-coding-toolkit` na revisão `21b71b68679c11184f49c23168852014bf6dde8b`. O toolkit foi usado como catálogo e método; não foi incorporado integralmente ao produto.

Baseline observado: Java 17, Node.js 22, npm 10 e Python 3.11. O projeto mantém Android/Kotlin/Compose, WebView, Supabase e a política de build no Mac Mini. Não houve migração de stack.

## Documentos canônicos

| Função | Arquivo |
|---|---|
| visão do repositório | `README.md` |
| produto e limites | `PRODUCT.md` |
| regras para agentes | `AGENTS.md` |
| entrada do Claude Code | `CLAUDE.md` |
| arquitetura | `docs/architecture/overview.md` |
| onboarding | `docs/onboarding.md` |
| design | `docs/design.md` |
| qualidade | `docs/quality.md` |
| setup e ferramentas | `docs/setup.md` |

## Seleção de ferramentas

- reutilizada: memória operacional em `.ai-context/`;
- adotado nativamente: planejamento/revisão por risco e comparação limitada de instruções;
- skills locais selecionadas: `gh-fix-ci`, `differential-review`, `supabase-postgres-best-practices`, `web-design-guidelines`, `writing-guidelines` e `find-skills`;
- Context7: sugerido para consulta de documentação atual, mas a conexão depende de confirmação no cliente;
- não selecionados: RTK, Graphify, browser MCP, Sentry, shadcn, Vercel deploy, skills React/Next e React Native, por não atenderem uma necessidade atual deste projeto;
- builds: nenhuma mudança; Mac Mini continua sendo o único destino autorizado.

Instalação de skill não significa que ela foi executada nem amplia autorização. Antes de usar uma skill, leia integralmente seu `SKILL.md` e aplique as regras de `AGENTS.md`.

As seis skills foram instaladas no escopo do projeto com `npx --yes skills@latest`
CLI 1.7.4 e registradas em `skills-lock.json`. A listagem reconhece todas no Codex;
cinco também têm links relativos em `.claude/skills/`. `gh-fix-ci` permanece somente
no Codex. A presença em disco comprova instalação, não execução nem descoberta em
uma sessão nova do cliente. O lockfile guarda hashes de conteúdo, mas algumas fontes
acompanham branches móveis; revise origem, diff e hash antes de qualquer atualização.

## Comparação inicial de instruções

A adaptação limitada do harness avaliou cinco falhas observadas com os mesmos
critérios antes e depois: link README portátil, onboarding canônico, política de
build no Mac Mini, ausência de prompts inexistentes e ausência de referências a
skills Haval inexistentes. O baseline em `HEAD` marcou 0/5; o candidato marcou 5/5.
Também passaram a validação de links Markdown, frontmatter das seis skills, links
relativos do Claude, `git diff --check` e a suíte `npm run check` de
`cluster-widgets`. Nenhum build Android, teste físico ou execução funcional das
skills foi realizado nesta configuração.

## Manutenção

Revise o diff e `skills-lock.json` ao atualizar skills. Prefira instalação local ao projeto e não altere preferências globais. Registre revisão de origem, resultado de descoberta e limitações neste arquivo. Publicação, commits, push, PRs, veículo e serviços externos permanecem ações separadamente autorizadas.
