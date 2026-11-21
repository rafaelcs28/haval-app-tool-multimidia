# Dashcam: galeria, reprodução, estado e automação

## Perfis de câmera

O perfil vem da sub-rede do Wi-Fi conectado (`DashcamProfile`):

| Perfil | Wi-Fi observado | Endereço | Protocolo | Lista | Escrita |
|---|---|---|---|---|---|
| HiSilicon | — | `192.168.0.1` | `/cgi-bin/hisnet/*.cgi` | só pausada | pausa/retomada/relógio validados |
| Novatek | `CARDV-3605` (carro, 2026-11-21) | `192.168.1.254` | `/?custom=1&cmd=N` (XML) | `3015` **gravando** | nenhuma até validar no carro |

Novatek, só leitura: `3037` modo (1 = vídeo), `2016` segundos do arquivo em gravação
(cresce enquanto grava), `3017` bytes livres no cartão, `3015` lista com `NAME`, `FPATH`
(`A:\CARDV\Movie\…TS`), `SIZE`, `TIME` (fim do arquivo) e `ATTR` (bit 0 = travado). Vídeos
`.TS` de 1 min (~139 MB) em `http://192.168.1.254/CARDV/Movie/<nome>.TS`, com `Range` (206)
mesmo gravando. Escrita Novatek: só `2001&par=0|1` (parar/gravar), usada pelas regras de pausa;
o efeito é conferido amostrando `2016` duas vezes (~1,2 s): o contador anda enquanto grava.
Retomada a cada 5 min e relógio continuam só na HiSilicon.

## Regras de pausa (`DashcamRecordingPolicy`)

- **Pausar durante recarga**: recarga ativa e velocidade ≤ 0,5 km/h.
- **Gravar só com o carro pronto**: `car.basic.driving_ready_state` diferente de 1 pausa, em
  qualquer marcha (ligado sem freio, recarga). Igual a 1 retoma.

Qualquer regra que peça pausa vence; telemetria desconhecida segura (HOLD). Só a pausa marcada
pelo Impulse (`pausedByApp`) é retomada; uma parada manual encontrada é preservada.

Aba **Dashcam** no menu lateral, entre Recursos e Reportar problema. Usa a mesma dashcam
HiSilicon da pausa durante recarga (`docs/architecture/dashcam-charging-pause.md`), em
`192.168.0.1:80`, sempre por um socket vinculado à rede Wi‑Fi Android (nunca VPN).

## Estado

`getworkstate.cgi?` é lido a cada 15 s enquanto a aba está aberta e pelo botão
**Consultar estado**, sem alterar a câmera. A tela distingue: Wi‑Fi não conectado, sem
resposta, gravando, emergência, pausada, pausada durante a recarga, pausada pela galeria e
outro modo. **Conectar à dashcam** só abre as configurações de Wi‑Fi; o app não associa redes.

O espaço livre do cartão aparece como indisponível: o relatório de protocolo não traz um
endpoint validado para ele.

## Galeria

O firmware testado responde `SvrFuncResult="-2222"` à listagem enquanto grava. Por isso:

1. **Atualizar galeria** primeiro tenta sem pausar. Se a câmera estiver gravando, nada é
   enviado e a tela pede confirmação para uma pausa curta.
2. Confirmada, a marca `pausedByGallery` é gravada (preferência protegida do dispositivo,
   fora de backup) **antes** do `stop`. Depois do `stop`, exige `running=false`.
3. Lista `norm` e `emr` em páginas de 50 (`-start/-end` inclusivos), com limite de 6 s,
   80 páginas por diretório e detecção de página repetida. Resultado incompleto é marcado
   como parcial.
4. Num bloco `finally`, retoma e só confirma após `running=true`; aí remove a marca.
5. Se a retomada não for confirmada, a tela mostra **Retomada não confirmada**, bloqueia
   nova pausa e oferece **Verificar e retomar**. O serviço também retoma essa pausa no
   próximo ciclo (ou a entrega à pausa da recarga, se a recarga pedir pausa naquele momento).

Pausa manual ou da recarga existente é preservada: lista sem enviar `stop`/`start`.
Gravação de emergência nunca é pausada.

Todas as sequências (recarga, galeria, automação) usam o mesmo `CAMERA_LOCK`, então pausa e
retomada de uma não se intercalam com outra.

Os caminhos aceitos seguem `sd//(norm|emr)/<nome>.MP4`; o resto é descartado e contado. A
última lista fica em `files/dashcam_catalog.json` (fora de backup) e é exibida enquanto a
câmera grava. A data vem do nome do arquivo (hora local da câmera). O fim de cada arquivo é
estimado pela cadência mediana entre arquivos (3 min sem cadência) e a tela informa isso.
Faixas juntam arquivos com intervalo de até 1,5× a duração estimada e não cruzam a meia-noite.

## Reprodução

Player Media3/ExoPlayer com `DashcamVideoDataSource`: `HttpURLConnection` aberto pela rede
Wi‑Fi da câmera, `Range` para avançar e voltar, sem redirecionamentos e somente para caminhos
validados do catálogo. Não pausa a câmera: o relatório mostra download com gravação ativa.
A reprodução só roda com velocidade conhecida ≤ 3 km/h; velocidade desconhecida ou acima
bloqueia e pausa o vídeo.

## Automação (todas desligadas por padrão)

A opção **Pausar dashcam durante recarga** (74162) saiu de Configurações → Recursos e fica
nesta seção, com a mesma chave de preferência: quem já tinha ligado continua ligado.

| Opção | Comportamento |
|---|---|
| Verificar e retomar a cada 5 minutos | Se a câmera estiver parada, envia `start` e confirma `running=true`. Respeita emergência, a pausa da recarga e o momento em que a recarga pede pausa. **Retoma também pausas manuais.** |
| Sincronizar o relógio da dashcam | `setsystime.cgi?&-time=yyyyMMddHHmmss` em hora local, ao encontrar a câmera e depois a cada 6 h. A hora vem de NTP (amostra guardada contra `elapsedRealtime`, pois o Wi‑Fi da câmera não tem internet) ou de um GPS com menos de 2 min. O relógio do Android sozinho não é usado. |
| Aviso de falha de gravação | Toast e notificação uma vez por episódio quando a câmera é encontrada sem gravar e não foi possível (ou não está ativado) retomar. Câmera fora de alcance não gera aviso. |

As opções seguem a convenção `ENABLE_`, então o Modo Concessionária as desliga junto.

## Logs

`cluster-diagnostics/cluster-events-AAAAMMDD.log`, eventos `dashcam_gallery` e
`dashcam_automation`: só fase, contagens e código de erro, sem corpo HTTP nem nomes de arquivo.

## Limites conhecidos

- Perfil validado em uma unidade; outros firmwares e endereços ficam fora.
- Reprodução no app, sequência de pausa da galeria, NTP e watchdog ainda exigem teste físico.
- O codec dos vídeos não foi confirmado; se o decodificador da central não suportar, o player
  mostra o erro.

## Salvar no carro e enviar ao celular

- **Permitir vídeo com o carro em movimento** (Automação, ligado por padrão): sem a trava de
  velocidade, para o passageiro. Desligado, volta a exigir velocidade conhecida ≤ 3 km/h.
- **Salvar no carro** (no player ou "Salvar faixa no carro"): `DashcamClipStore` baixa pela
  rede da câmera, um arquivo por vez, para `Android/data/<app>/files/Dashcam/`, gravando em
  `.part` e renomeando só quando o tamanho bate com o `Content-Length`. Mantém 300 MB livres.
- **Salvos no carro**: assistir (arquivo local), enviar ao celular ou apagar.
- **Enviar ao celular**: `DashcamShareServer` serve **um** arquivo no IP Wi-Fi da central por
  15 min, em `/<token de 128 bits>/<nome>`, com `Range`. Recusa a rede da câmera. O QR code é
  gerado no aparelho (ZXing), então o link com o token não sai do carro.
