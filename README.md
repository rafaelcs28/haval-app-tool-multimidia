# Haval Impulse

Aplicativo Android não oficial para estudo e extensão da central multimídia Haval/GWM. O projeto reúne um host Android em Kotlin/Java, UI em Compose, temas HTML executados em WebView, integração por Shizuku e experimentos isolados de projeção no cluster.

O projeto não é afiliado, patrocinado ou aprovado pela Haval, GWM ou empresas relacionadas. É software experimental, sem garantia. Operações em veículo devem ser feitas apenas pelo proprietário ou por pessoa expressamente autorizada, em condições seguras.

## Começando

1. Leia [AGENTS.md](AGENTS.md) e o topo de [.ai-context/HANDOFF.md](.ai-context/HANDOFF.md).
2. Siga [docs/onboarding.md](docs/onboarding.md) para preparar o ambiente.
3. Consulte [docs/architecture/overview.md](docs/architecture/overview.md) antes de alterar contratos, displays ou bridges.
4. Use [docs/quality.md](docs/quality.md) para escolher a validação proporcional ao risco.

Builds Android, Gradle e Kotlin são executados exclusivamente na fila autorizada do Mac Mini, a partir de um commit Git imutável e auditado. Não rode Gradle no host coordenador e não faça fallback local.

## Estrutura

- `app/`: aplicativo Android e recursos embarcados.
- `cluster-widgets/`: fontes e artefatos dos temas do cluster.
- `docs/architecture/`: decisões e contratos técnicos canônicos.
- `docs/operations/`: procedimentos operacionais.
- `tools/headunit-dev/`: ferramentas de diagnóstico e implantação autorizada.
- `.ai-context/`: estado corrente, pendências e histórico de handoff.

## Documentação

- [Produto e limites](PRODUCT.md)
- [Arquitetura](docs/architecture/overview.md)
- [Design e temas](docs/design.md)
- [Qualidade e evidências](docs/quality.md)
- [Ferramentas e configuração](docs/setup.md)

## Licença

Consulte [LICENSE](LICENSE). O uso de componentes e marcas de terceiros continua sujeito aos respectivos termos.
