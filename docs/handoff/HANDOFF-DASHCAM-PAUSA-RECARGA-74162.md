# Handoff — pausa da dashcam durante recarga (74162)

## Estado entregue

- Repositório: `rafaelcs28/haval-app-tool-multimidia`.
- Branch: `feature/dashcam-pause-while-charging`.
- Fonte da release: `64dd015d726a03f4b9ae22c68fab7cb6115bdb46`.
- OTA: `1.0.0.74.162-preview`, `versionCode=74162`.
- APK: `impulse-74162.apk`, 104492319 bytes.
- SHA-256: `9ec8e3582f83abe8f9b579d567c461dea6014ea40709b936ef6aadfa1a58d090`.
- Certificado SHA-256: `5efec0f94d84d90e769635a81d51e2f25953d5d5b3cea950a115705d3b5e83e0`.
- Instalada na central em 2026-11-21, sem reiniciar o Android. Após a instalação,
  `dumpsys package` confirmou `versionCode=74162` e o processo do Impulse estava ativo.

## Escopo exato

A 162 adiciona somente a opção **Pausar dashcam durante recarga**. A opção vem
desativada. Quando ativada, pausa a gravação somente se a recarga estiver ativa e a
velocidade conhecida for menor ou igual a 0,5 km/h.

Não foram adicionados galeria, reprodução, download de vídeos, estado visual da câmera,
controle manual de gravação nem configuração geral da dashcam. O relatório de protocolo
foi usado como evidência para este recorte; ele não foi transformado inteiro em produto.

## Caminho para ativar

1. Abra o **Impulse**.
2. Entre em **Configurações**.
3. Localize o grupo **Recursos**.
4. Ative **Pausar dashcam durante recarga**.
5. Mantenha a central conectada ao Wi-Fi da dashcam HiSilicon.

O Impulse não troca nem associa a rede Wi-Fi automaticamente.

## Regras de segurança e recuperação

- `car.ev_info.charging_state == 1` e `car.basic.vehicle_speed <= 0,5` permitem pausar.
- Telemetria ausente, inválida ou desconhecida não envia comando.
- O estado precisa ser `NORM_REC`, com `running=true` e `emrrecord=false`.
- Gravação de emergência nunca é pausada.
- Pausa manual existente é preservada.
- O app retoma somente uma pausa marcada como iniciada pelo próprio Impulse.
- Cada comando exige HTTP 200, corpo `Success` e nova leitura confirmando o estado.
- Falhas de transporte ou verificação são tentadas novamente após 30 segundos.
- A marca `pausedByApp` fica em preferência protegida do dispositivo e é excluída de
  backup/transferência para não migrar para outra central.

## Protocolo e rede

O cliente abre socket na rede Android `TRANSPORT_WIFI`, exclui VPN e acessa somente
`192.168.0.1:80`. Usa HTTP/1.0, `Connection: close`, timeout de 3 segundos, resposta
máxima de 64 KiB e não segue redirecionamentos.

| Operação | Endpoint |
|---|---|
| Ler estado | `/cgi-bin/hisnet/getworkstate.cgi?` |
| Pausar | `/cgi-bin/hisnet/workmodecmd.cgi?&-cmd=stop` |
| Retomar | `/cgi-bin/hisnet/workmodecmd.cgi?&-cmd=start` |

## Arquivos principais

- `app/src/main/java/br/com/redesurftank/havalshisuku/managers/DashcamChargingPauseManager.java`
- `app/src/main/java/br/com/redesurftank/havalshisuku/managers/DashcamChargingPausePolicy.java`
- `app/src/main/java/br/com/redesurftank/havalshisuku/managers/DashcamHisiliconClient.java`
- `app/src/main/java/br/com/redesurftank/havalshisuku/managers/DashcamHisiliconProtocol.java`
- `app/src/main/java/br/com/redesurftank/havalshisuku/services/ForegroundService.java`
- `app/src/main/java/br/com/redesurftank/havalshisuku/models/SharedPreferencesKeys.kt`
- `app/src/main/java/br/com/redesurftank/havalshisuku/ui/screens/BasicSettingsScreen.kt`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `docs/architecture/dashcam-charging-pause.md`

## Testes e build

- TDD RED: `efde382e8baed89fd24e3a71287707e25b55dc2b` falhou porque política e parser ainda não existiam.
- Implementação: `5f8307d47d4b57cb9a08b90bdfb085a15b94336f`.
- Correção de inicialização: `d7d3037f14eb98aa99211f8c2b5efa3d50e203a5`.
- Fonte final: `64dd015d726a03f4b9ae22c68fab7cb6115bdb46`.
- Debug final no Mac Mini: job `haval-impulse-133-64dd015d726a03f4-20261121T153212Z-15216`, PASS.
- Release final no SSD externo do Mac Mini: job
  `haval-impulse-133-64dd015d726a03f4-20261121T153507Z-31073`, PASS.
- JVM, código nativo, R8, lint vital, assinatura e `assembleRelease`: PASS.
- Download público integral e manifesto `app-latest/app-preview.json`: SHA e tamanho conferidos.

Testes unitários:

- `DashcamChargingPausePolicyTest.java`: pausa, retomada, HOLD e telemetria inválida.
- `DashcamHisiliconProtocolTest.java`: parser HTTP/estado, emergência, respostas incompletas,
  `Success` exato e rejeição de redirecionamento.

## Próximo teste físico

1. Conecte a central ao Wi-Fi da dashcam.
2. Com o veículo parado, inicie uma recarga externa.
3. Ative a opção no Impulse.
4. Confirme na câmera que a gravação parou.
5. Encerre a recarga ou mova o veículo.
6. Confirme que a gravação voltou.
7. Repita com uma pausa manual anterior; o Impulse não deve retomá-la.
8. Repita durante gravação de emergência; o Impulse não deve pausá-la.

Eventos técnicos ficam em `cluster-diagnostics/cluster-events-AAAAMMDD.log`, com nome
`dashcam_charging_pause`. Os logs registram somente fase e código de erro, sem corpo HTTP.

## Limites conhecidos

- Validado em testes de software, mas a atuação física ainda não foi comprovada.
- O perfil foi observado em uma dashcam HiSilicon. Outros endereços e firmwares ficam fora.
- Se o processo não voltar após uma pausa própria, a retomada depende da próxima execução
  do Impulse ou do comportamento de reinício da câmera.
