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
  `.part` e renomeando só quando o tamanho bate com o `Content-Length`. Mantém 1 GB livre (abaixo de ~500 MB o Android recusa instalar apps).
- **Salvos no carro**: segunda visão no topo da aba (Câmera | Salvos no carro), com espaço usado e livre; assistir, enviar ao celular (QR) ou apagar.
- **Enviar ao celular**: `DashcamShareServer` serve **um** arquivo no IP Wi-Fi da central por
  15 min, em `/<token de 128 bits>/<nome>`, com `Range`. Recusa a rede da câmera. O QR code é
  gerado no aparelho (ZXing), então o link com o token não sai do carro.

## Conversão para MP4

Os vídeos Novatek são `.TS` (MPEG-TS), que a galeria do celular não importa. Ao terminar de
salvar um `.TS`, `DashcamClipStore` faz um remux só da faixa de vídeo para `.mp4`
(`MediaExtractor` → `MediaMuxer`, sem recodificar; começa no primeiro quadro-chave). O áudio é
descartado: o dono não grava áudio. Se o remux falhar, o `.TS` é mantido. O espaço exigido
dobra durante a conversão. O "Enviar ao celular" entrega o `.mp4` (`video/mp4`).

## Estado para o Haval 3D

`DashcamStatusPublisher` consulta a câmera a cada 30 s (só leitura, mesmo `CAMERA_LOCK`) e
publica no barramento público `com.haval.vehicle.EVENT_CHANGED`, só quando o valor muda e de novo
em todo `REQUEST_SNAPSHOT`: `app.dashcam.state` (recording | paused | paused_auto | emergency |
other_mode | unreachable | no_wifi | unknown), `app.dashcam.pause_reason` (charging | ready),
`app.dashcam.profile`, `app.dashcam.free_mb` (último conhecido), `app.dashcam.total_mb` (capacidade do cartão = livre + clipes listados; só Novatek, relida a cada 6 h), `app.dashcam.saved_count`,
`app.dashcam.saving` ("NN" | converting) e `app.dashcam.checked_at`. O toque no card abre
`OpenDashcamActivity` (exportada, só abre a aba Dashcam; ignorada no Modo Concessionária).

Rede (a cada leitura da câmera, sem tráfego extra para ela; mesmas regras de envio):

- `app.dashcam.link`: `camera_ap` (central no Wi-Fi da própria câmera: 192.168.1.x, o SSID do
  Wi-Fi da câmera lembrado, ou uma HiSilicon que respondeu), `same_network` (central na rede
  configurada no card "Rede da câmera" e a câmera respondeu), `camera_missing` (na rede
  configurada, mas a câmera não respondeu), `other_wifi` (qualquer outro Wi-Fi) ou `no_wifi`.
  Com o SSID oculto pelo Android, uma câmera que respondeu fora do Wi-Fi dela conta como
  `same_network`. Regra pura em `DashcamStatusPublisher.linkValue` (testada).
- `app.dashcam.wifi_ssid`: SSID atual da central sem aspas ("" sem Wi-Fi ou `<unknown ssid>`).
- `app.dashcam.camera_ip`: endereço da câmera em uso (192.168.1.254 no Wi-Fi dela, 192.168.0.1 na
  HiSilicon, o IP do DHCP na rede configurada); "" quando desconhecido.

O Haval 3D antigo ignora as três; sem `app.dashcam.link`, o card do 3D mantém o comportamento
anterior.

## Trajeto GPS dos vídeos (174)

`DashcamGpsTrack` grava o GPS da própria central (1 Hz, `LocationManager.GPS_PROVIDER`) com o
horário que o GPS informa (`Location.getTime()`, UTC dos satélites), nunca o horário de chegada.
Fica só no carro, em `files/dashcam-gps/AAAAMMDDHH.csv` (hora UTC), por 72 h; nada é enviado
para fora. Chave: "Localizar os vídeos no mapa (GPS)" (`enableDashcamGpsTrack`, padrão ligado).

O clipe é casado pelo início no nome (`20261121152923_…`, hora local da câmera, lida no fuso da
central) mais 60 s. Isso só fica exato com o relógio da câmera em dia: "Sincronizar o relógio da
dashcam" agora vale também para a Novatek (`3005&str=AAAA-MM-DD`, `3006&str=HH:MM:SS`, a cada 6 h,
com a hora confiável de NTP ou GPS de `DashcamTimeSource`). O último ajuste aceito fica em
`dashcam_state/clock_synced_at`; sem ele o painel avisa que a posição pode estar deslocada.

No player, o painel "Trajeto GPS" desenha a linha do clipe, com o ponto acompanhando a reprodução,
a velocidade, as coordenadas e um QR que abre o ponto no mapa do celular. Ao salvar no carro, o
clipe ganha `<nome>.gpx` (trajeto com hora do GPS) e o MP4 leva a localização de início
(`MediaMuxer.setLocation`), então a galeria do celular mostra o vídeo no mapa. Apagar o clipe apaga
o `.gpx`.

## Mapa e carimbo (175–177)

Aba **Mapa** (`DashcamMapCard`): tiles do OpenStreetMap em `files/map-tiles` (até 200 MB, 30 dias;
`MapTiles`), baixados ao abrir o mapa e, ao longo do trajeto do último dia, só em Wi-Fi sem
franquia. Endereço pelo Nominatim (1 pedido/s, cache). Trajeto colorido: azul no cartão da câmera,
verde salvo, cinza gravado por cima. Tocar no trajeto ou arrastar a barra escolhe o momento;
"Assistir" abre o player naquele segundo, "Salvar" baixa o minuto (ou com o anterior e o seguinte).

**Dados do carro** (`DashcamCarTrack`, 1 Hz, mesmos 72 h e mesma chave do GPS): velocidade, marcha,
alerta de cinto, setas, pisca-alerta, faróis, freio de mão, volante, modo, bateria, potência,
temperaturas, hodômetro. Valor bruto por coluna; arquivo começa com `#keys`.

**Carimbo** ("Gerar com carimbo" em Salvos): `DashcamStampJob` monta `DashcamStampFrame` a cada 0,5 s
(aceleração em g pela variação da velocidade em ~2 s), desenha com `DashcamStampPainter` e
`DashcamStampEncoder` recodifica no hardware (MediaCodec → OpenGL com o carimbo → H.264), gerando
`<clipe>_carimbo.mp4`. O original não é alterado.

Na 177, "Sincronizar o relógio da dashcam" é ligado uma vez para todos (pedido do dono), e todas
as opções da Dashcam têm padrão ligado.

## Rede da câmera: levar a Novatek para outra rede (185)

A Novatek não tem botões. O card **Rede da câmera** (aba Câmera, acima de Automação) a leva do
Wi-Fi próprio para uma rede compartilhada ("station", ex.: roteador do celular) e de volta.
Sequência validada no carro (firmware TY670DLA335):

- **Para a rede**: com a central no Wi-Fi da câmera, `3016` (responde), `3032&str=SSID:SENHA`
  (cada metade com percent-encoding, `:` literal; SSID sem `:`, senha de 8 a 26 caracteres),
  `3033&par=1`, `3018`. Cada um exige `Status 0` (no `3018` a conexão pode cair antes da
  resposta: conta como enviado). Depois a central entra na rede (`WifiManager.addNetwork`/
  `enableNetwork`, WPA2, reaproveitando a configuração salva com esse SSID) e procura a câmera por
  até 90 s.
- **De volta**: na rede, com a câmera encontrada, `3033&par=0` e `3018`; a central volta ao SSID do
  Wi-Fi da câmera, lembrado quando ela esteve nele.

Pré-requisitos do envio: perfil Novatek, central no 192.168.1.x, SSID/senha válidos e a rede
visível em `getScanResults()`. SSID e senha ficam em `haval_prefs` (`dashcamStationSsid`,
`dashcamStationPassword`); nunca vão para log. Comandos sob `CAMERA_LOCK`; as esperas de Wi-Fi não
seguram a trava. Eventos `dashcam_network` só com fase e código de erro.

**Endereço dinâmico** (`DashcamCameraHost`): 192.168.1.254 no Wi-Fi da câmera; na rede
configurada, o IP do DHCP. Na rede, o perfil é Novatek quando o SSID atual é o configurado (ou,
sem SSID legível, quando o IP guardado está na mesma /24) e a câmera já é conhecida (MAC ou IP).

**Descoberta** (`DashcamCameraDiscovery`): no Wi-Fi da câmera, o MAC de 192.168.1.254 é lido de
`/proc/net/arp` e guardado em `dashcam_state` com o SSID do Wi-Fi da câmera. Na rede: MAC no ARP;
se não estiver, varredura TCP 80 concorrente da /24 (~3 s) e novo ARP; por fim `3016` nos hosts
com a porta 80 aberta (até 16). O IP fica em cache; falha de transporte/HTTP o descarta e a próxima
consulta redescobre (varredura no máximo 1×/min, fora as trocas de rede).

**Cleartext**: o `network_security_config` continua permitindo só 192.168.0.1 e 192.168.1.254.
Vídeo (`DashcamVideoDataSource`) e download (`DashcamClipStore`) da Novatek usam
`DashcamCameraHttp`: HTTP/1.1 por socket da rede Wi-Fi (nunca VPN), só IPv4 privado literal igual
ao host atual da câmera, só caminhos `/CARDV/…` validados, `Range`, sem redirecionamento, sem
`Transfer-Encoding`, cabeçalho limitado a 16 KB e corpo limitado ao `Content-Length`. A HiSilicon
continua com `HttpURLConnection`.

**Automático** ("Fazer isso sozinho a cada partida", `enableDashcamAutoStation`, **padrão
desligado**): no ciclo da automação (30 s após iniciar e a cada 5 min), no Wi-Fi da câmera com a
rede ao alcance → leva a câmera; na rede sem a câmera mas com o Wi-Fi dela ao alcance → a central
volta ao Wi-Fi da câmera e a leva de novo. No máximo uma tentativa por partida e uma a cada 10 min.
Relatos da comunidade dizem que o modo station não persiste ao desligar o carro (ainda não
confirmado nesta câmera).

**Risco**: na rede compartilhada a câmera não tem senha; qualquer aparelho dela pode ver, apagar e
reconfigurar. O card avisa.

**Pré-configuração só pelo adb** (`DashcamStationConfigReceiver`): para preencher a rede numa
central específica sem embutir nenhuma rede no app. O receiver é exportado, mas protegido por
`android.permission.DUMP` (só o shell do adb e o sistema têm; apps de terceiros não conseguem).
Valores de exemplo; troque NOME e SENHA pelos da rede:

```
adb shell am broadcast -a br.com.redesurftank.havalshisuku.DASHCAM_STATION_CONFIG \
  -n br.com.redesurftank.havalshisuku/.broadcastReceivers.DashcamStationConfigReceiver \
  --es ssid NOME --es password SENHA --es auto 1
```

`ssid` e `password` vão juntos e passam pelas regras de `DashcamStationPolicy.validate` (SSID sem
`:`, até 32 bytes; senha de 8 a 26 caracteres ASCII). `auto` é opcional (`1`/`true`/`0`/`false`,
ou `--ez auto true`) e liga/desliga "Fazer isso sozinho a cada partida"; pode ir sozinho. Grava em
`haval_prefs` (as mesmas chaves do card, que mostra os valores ao ser aberto). O resultado do
`am broadcast` traz `ok: ssid set, auto=…` ou `error: <código>`; o log só registra
"ssid set, auto=…", nunca o SSID nem a senha.

Permissões novas: `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` (SSID e varredura também dependem da
localização, já concedida para o GPS).

### Modo automático e comandos por adb (187)
- O automático tem a sua própria checagem **a cada 1 min** e age em dois casos só: central **no Wi-Fi da
  câmera** com o SSID configurado visível (leva a câmera); ou central **no SSID configurado**, câmera sem
  responder ali e o **Wi-Fi da câmera visível** (a central entrou na rede antes da câmera: pula para o Wi-Fi
  da câmera e leva a câmera, 188). Em qualquer outra rede, nada. Falhou: espera 5 min.
  "Trazer câmera para o Wi-Fi próprio" (card ou adb) segura o automático por 30 min.
- Comandos pela mesma entrada protegida (DUMP): `--es command move` (levar) e `--es command return`
  (trazer). Respondem "started"; o resultado sai no log `DashcamNetwork` e no card.

### 190: pulo segura o Wi-Fi da câmera

No pulo (central no SSID configurado, câmera ausente), o Android largava o Wi-Fi da câmera ~6 s depois de
entrar (sem internet, com a rede configurada ao alcance) e voltava à rede configurada antes de a câmera
responder. Durante o pulo o app desliga a validação de internet (`captive_portal_mode` = 0, valor original
guardado em `dashcam_state` e restaurado no fim, ou na próxima inicialização se o processo morrer) e mantém um
pedido de rede Wi-Fi sem INTERNET. Os dois são soltos assim que a câmera recebe os comandos. O heartbeat tenta
6 vezes (189) e o card "Rede da câmera" mostra a última troca automática.

### 198: comandos espaçados e troca em etapas

Teste de campo 24/11: com 3032, 3033 e 3018 colados (~0,03 s), a câmera entra no hotspot mas não responde a
ninguém (nem ao celular); com ~3 s entre eles ela entra e responde (10.154.131.78). Agora há 3 s entre cada
comando e cada etapa feita fica lembrada (10 min): se o Android tira a central do Wi-Fi da câmera no meio
(3-4 s depois de um pulo automático), a troca volta como "in_progress", o automático tenta de novo em 5 s e
continua da etapa seguinte. O 3018 só sai com a central ainda no Wi-Fi da câmera.

