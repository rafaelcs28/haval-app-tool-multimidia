# Pausa da dashcam durante recarga

## Escopo

A opção **Pausar dashcam durante recarga** pausa a gravação da dashcam HiSilicon
somente quando estas condições são conhecidas ao mesmo tempo:

- `car.ev_info.charging_state == 1`;
- `car.basic.vehicle_speed <= 0,5 km/h`;
- a central possui uma rede Wi-Fi ativa que alcança a câmera;
- `getworkstate.cgi?` identifica o perfil esperado `NORM_REC`;
- a câmera informa `running=true` e `emrrecord=false`.

Telemetria ausente, inválida ou desconhecida não altera a câmera. Desativar a opção,
encerrar a recarga ou detectar movimento solicita a retomada somente quando a pausa
foi iniciada pelo Impulse.

## Protocolo usado

O cliente usa HTTP/1.0 diretamente sobre um socket vinculado à rede Wi-Fi Android.
O destino é fixo em `192.168.0.1:80`; não há DNS, proxy nem redirecionamento.

| Operação | Caminho |
|---|---|
| Estado | `/cgi-bin/hisnet/getworkstate.cgi?` |
| Pausar | `/cgi-bin/hisnet/workmodecmd.cgi?&-cmd=stop` |
| Retomar | `/cgi-bin/hisnet/workmodecmd.cgi?&-cmd=start` |

Antes de qualquer escrita, o cliente exige HTTP 200 e os três campos reconhecidos do
estado. Depois de cada comando, exige corpo `Success` e consulta novamente o estado.
Respostas acima de 64 KiB, HTTP malformado, outros modos e gravação de emergência
falham sem alterar o estado pretendido.

## Propriedade da pausa e recuperação

O Impulse grava `pausedByApp` em `dashcam_state` antes de enviar `stop`. Isso cobre a
situação em que a câmera executa o comando, mas a resposta ou a verificação se perde.
A marca só é removida depois de confirmar `running=true`.

Uma pausa já existente, sem a marca do Impulse, é preservada. Falhas de transporte ou
verificação mantêm a marca e são tentadas novamente a cada 30 segundos. O gerenciador
usa uma thread própria para não bloquear callbacks do veículo ou a interface.

## Limites conhecidos

- O app não associa nem troca a rede Wi-Fi automaticamente.
- O perfil foi observado em uma unidade; outros firmwares e endereços não são tratados.
- Se o processo não voltar a executar depois de uma pausa própria, a retomada depende
  da próxima inicialização do Impulse ou do comportamento de boot da câmera.
- Testes JVM validam decisão e parser. Pausa, retomada e rota Wi-Fi exigem teste físico.
