# Session Workflow

## Início

O agente deve ler:

- `AGENTS.md`
- o bloco inicial de `.ai-context/HANDOFF.md`
- `.ai-context/KNOWN-ISSUES.md` e `.ai-context/NEXT-STEPS.md` quando pertinentes
- `docs/architecture/overview.md` e a documentação do subsistema afetado

Antes de confiar no handoff, confirmar `git status`, branch, `HEAD`, arquivos e
processos reais. Texto histórico não prova o estado atual.

## Durante a Sessão

- Mapear antes de editar.
- Usar agents para tarefas complexas.
- Usar skills por domínio.
- Implementar somente o necessário.
- Validar com comandos disponíveis.

## Encerramento

Atualizar:

- `.ai-context/HANDOFF.md`
- `.ai-context/CHANGELOG-AI.md`
- `.ai-context/NEXT-STEPS.md`
- `.ai-context/KNOWN-ISSUES.md`, se necessário.

Registrar apenas comandos realmente executados, resultados observados, riscos e a
próxima ação. Não criar um novo prompt de sessão quando este fluxo for suficiente.
