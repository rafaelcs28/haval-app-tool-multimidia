# Design e temas

Este documento é um índice de decisões visuais; os contratos detalhados continuam canônicos nos documentos de arquitetura.

## Princípios

- preservar legibilidade e hierarquia na resolução real do cluster;
- evitar layout shift, animações custosas e trabalho periódico desnecessário;
- manter estados de carregamento, erro e ausência de dados explícitos;
- não alterar limites da barra de status nem o contrato da bridge sem revisão de compatibilidade;
- validar no simulador e, quando a afirmação depender dele, no veículo.

## Referências canônicas

- `docs/architecture/themes-contract-v1.md`: contrato de temas e bridge;
- `docs/architecture/overview.md`: host Android, WebView e displays;
- `docs/carplay-cluster-regression-contract.md`: isolamento e regressões de CarPlay;
- `cluster-widgets/README.md`: organização e comandos dos temas.

Temas novos devem partir da versão de contrato suportada mais recente. O tema padrão é embarcado no APK; temas dinâmicos seguem o fluxo OTA documentado no repositório.
