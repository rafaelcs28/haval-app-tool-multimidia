# Produto

Este documento define o público, as capacidades, os limites e o critério de aceite do Haval Impulse.

## Problema

O Haval Impulse concentra ferramentas experimentais para observar, personalizar e estender a central multimídia e o display secundário de veículos Haval/GWM compatíveis. Ele reduz procedimentos manuais dispersos e mantém diagnósticos reproduzíveis para integrações sensíveis como temas, Android Auto e CarPlay.

## Público

- mantenedores e colaboradores técnicos;
- proprietário do veículo de teste e pessoas expressamente autorizadas;
- autores de temas compatíveis com o contrato publicado.

## Capacidades principais

- exibir temas HTML no cluster por meio de um host Android/WebView;
- observar telemetria e eventos da central;
- administrar temas e aplicativos compatíveis;
- executar diagnósticos controlados de Android Auto e CarPlay;
- registrar evidências privadas suficientes para reproduzir falhas.

## Restrições obrigatórias

- firmware, hardware e comportamento OEM variam; inferências precisam de evidência de campo;
- Android Auto e CarPlay permanecem isolados salvo pedido explícito;
- contratos Android/WebView, resoluções, displays e bridges devem manter retrocompatibilidade;
- suíte automatizada não substitui validação física quando a afirmação depende do veículo;
- ações no veículo, publicação, assinatura e alterações externas exigem autorização própria;
- builds Android/Gradle/Kotlin usam somente o Mac Mini autorizado e um commit Git imutável.

## Fora de escopo

- substituir sistemas críticos ou controles de segurança do veículo;
- garantir compatibilidade universal entre firmwares;
- distribuir componentes proprietários de terceiros;
- contornar gates de segurança para obter uma demonstração visual.

## Critério de aceite

Uma mudança está pronta quando preserva os contratos aplicáveis, possui validação proporcional ao risco, registra limitações e evidências e não apresenta resultado físico como comprovado antes do teste correspondente.
