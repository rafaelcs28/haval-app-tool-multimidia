# Captura de campo — Android Auto no cluster

## Objetivo

Correlacionar uma eventual imagem congelada do Waze com o fluxo CLUSTER da `1.0.0.74.118-preview`. O coletor é somente leitura na multimídia: não reinicia processos, não altera propriedades e não instala arquivos.

## Antes de sair

1. Ligue a multimídia e confirme que o Tailscale está conectado.
2. Avise nesta conversa antes de iniciar a projeção.
3. Na VPS, inicie:

```bash
cd /root/projects/haval-impulse
scripts/capture-aa-cluster-drive.sh --duration 7200 --wait 1800
```

O script espera `haval-central`, valida a porta Telnet 23 e cria uma pasta em `test-artifacts/aa-cluster-drive/<data-hora>/`.

## O que é monitorado

- `logcat` completo de todos os buffers, com visão filtrada para `AaClusterCtrl`, `ImpulseAaCluster`, `ClusterPersist`, codec, SurfaceFlinger, ANR e exceções.
- Evento persistente `aa_cluster_frame_stall_recovery`, incluindo idade do último frame, velocidade e contador de recuperação.
- Estados CLUSTER (`WAITING_*`, `LIVE`, `FAILED`, `DISABLED`) e trocas de output.
- Frames recebidos do telefone e criação/fechamento do decoder AVC.
- Superfícies relacionadas ao Android Auto, cluster e Presentation.
- Estado do `media.codec`, PIDs do Impulse/Android Auto e serviços Android relevantes.
- Latência e disponibilidade do enlace Tailscale no início e no fim.
- Versão instalada do APK, que deve ser `versionCode=74118`.

## Se o mapa travar

Não interaja com o chat enquanto estiver dirigindo. Quando for seguro, informe o horário aproximado ou marque imediatamente na VPS:

```bash
cd /root/projects/haval-impulse
scripts/mark-aa-cluster-event.sh travou
```

Também podem ser marcados eventos de comparação:

```bash
scripts/mark-aa-cluster-event.sh projecao-iniciada
scripts/mark-aa-cluster-event.sh mapa-normal
scripts/mark-aa-cluster-event.sh rota-encerrada-e-retomada
```

## Interpretação principal

- `aa_cluster_frame_stall_recovery` seguido de novo `LIVE`: watchdog detectou e recuperou o stream.
- Evento de recuperação sem novo `LIVE`: reciclagem iniciou, mas o stream/decoder não retomou.
- Congelamento sem evento, com velocidade acima do limite: investigar atualização de `SurfaceTexture`, estado do controller e timestamp do último frame.
- Frames `ImpulseAaCluster` continuam chegando, mas a superfície não atualiza: suspeita no decoder/renderização.
- Frames deixam de chegar e link/TBT continuam ativos: confirma o cenário de stall isolado do stream CLUSTER.
