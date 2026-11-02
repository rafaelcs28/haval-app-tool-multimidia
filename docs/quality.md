# Qualidade e evidências

Este guia define qual evidência cada tipo de mudança precisa e impede que testes locais sejam apresentados como prova física.

## Camadas de evidência

1. revisão estática e diff;
2. checks portáteis e testes unitários do fluxo;
3. build Android no Mac Mini a partir de commit imutável;
4. verificação de artefato, assinatura, versão e checksum;
5. teste físico autorizado quando a conclusão depende da central ou do veículo.

Uma camada posterior não apaga limitações das anteriores. Teste verde não prova imagem no cluster; `surface_ready` não prova primeiro frame apresentado.

## Comandos

- documentação: `git diff --check` e verificação de links relativos;
- temas: scripts npm declarados no diretório afetado;
- Python/Node/native: somente suítes documentadas para o componente;
- Android/JVM/Kotlin: `scripts/build-on-mac-mini.sh <COMMIT>`.

Nunca rode Gradle no coordenador e nunca faça fallback automático se a fila estiver indisponível.

## Revisão por risco

- documentação: links, consistência, instruções executáveis e ausência de segredos;
- UI/temas: contrato, acessibilidade, resolução, performance e artefato gerado;
- bridge/display/projeção: compatibilidade, isolamento AA/CarPlay, falha fechada e logs sem payload sensível;
- publicação/veículo: autorização, alvo exato, checksum e confirmação pós-ação.

## Harness adaptado

O projeto usa uma adaptação nativa e limitada do método Harness2: definir uma falha observável, comparar a instrução anterior e a candidata sob os mesmos critérios e guardar o resultado. Isso não instala nem declara compatibilidade com um framework oficial Harness2.

Registre comandos, resultados, riscos e o que não foi testado em `.ai-context/CHANGELOG-AI.md` e atualize o handoff no fim da sessão.
