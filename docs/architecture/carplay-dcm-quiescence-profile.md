# Evidência de ausência no DCM da central

O preparo automático usa o serviço
`com.ts.car.dcm.dccontroller/.connectionmanagerservice.CmService` apenas para leitura.
A interpretação é específica do APK OEM de 237.975 bytes, SHA-256
`7859c63e089d6f053df7b4121063e30d4e5f7795cf8fca52a73bebe8a44b9b69`.

`CarPlayBridgeQuiescence` resolve esse serviço no PackageManager e verifica o arquivo
`ApplicationInfo.sourceDir` local. A aprovação fica em memória por identidade do
arquivo e época do Binder; nova conexão, caminho, tamanho ou data de modificação
exigem nova conferência. Falha de leitura ou firmware diferente retorna desconhecido.
Não há root, rede ou comando de recuperação nessa leitura.

## Semântica comprovada no APK

Na transação 14, `getOnlineDevices(Bundle)` inclui somente dispositivos para os
quais `Device.isOnline()` é verdadeiro. Esse método exige mapa de conexões não
vazio e estado do dispositivo diferente de 2, 3 e 4. Portanto, uma entrada online
não é simplesmente um telefone salvo. Mesmo Bluetooth sem projeção impede este
preparo conservador; os telefones devem estar completamente desconectados.

Na transação 45, `getActiveProjections()` delega a
`ProjectionManager.getProjectionDevices()`:

| Resposta na wire | Significado no firmware verificado | Tratamento |
| --- | --- | --- |
| Lista nula, tamanho `-1` | Mapa de conexões de projeção vazio | Evidência de ausência |
| Lista não nula vazia, tamanho `0` | Mapa não vazio, mas sem dispositivo ativo retornado | Desconhecido; bloqueia |
| Lista com dispositivos válidos | Dispositivos de projeção observados | Ocupado; bloqueia |
| Erro, tamanho inválido ou bytes restantes | Resposta não comprovada | Desconhecido; bloqueia |

A lista vazia não prova inatividade: `makeProjectionConnection()` pode adicionar
uma conexão após `activateConnection()` apenas enfileirar o evento de preconexão,
antes de existir um dispositivo ativo. O leitor não admite essa janela.

O resultado final só afirma ausência com Bundle online presente, vazio e
completamente decodificado, além da lista nula comprovada. Continuam obrigatórios
cinco segundos com o mesmo Binder e pelo menos três respostas consecutivas frescas
ao longo de dois segundos. O supervisor também exige suas próprias verificações de
P, velocidade zero, identidade do OEM e ausência de telas de projeção. Não existe
neste SDK um sinal explícito de inicialização concluída; o tempo de espera é uma
política conservadora, não esse sinal.

## Evidências e limites

REA no APK capturado, sem executá-lo:

- `ev_b95ce38630def79164ace7bdfd7d8fc109f20ec084da9dc2982727b7a2928165`: delegação da transação 45.
- `ev_af62956ecfce58227b9e1cf0362518f3bfe330fbecdef166365363d1433b2ffc`: ramos lista nula e lista não nula.
- `ev_20169cd3b3f4ab42239d3d929ed3aff0f46643bfeeb395f470a3bbfc890655a2`: seleção de dispositivos online.
- `ev_d3c75a7e21d721b50e825447fa49055fd1f209d77afbc9b2466811f27b790491`: predicado `isOnline()`.
- `ev_341e01af88fd177c92a6f07299df3a319883e057643dc7bae81e0291ce21011a` e `ev_b47df538ee505f9a721ce61a155594f0662ba6f9b1ba54cca6906c6e3249ef34`: conexão registrada após enfileirar preconexão.

O APK release anterior também renomeou `ConnDevice` pelo R8, impedindo o Bundle
externo de localizar esse Parcelable pelo nome OEM. A regra de preservação de nome
é necessária para respostas não vazias; ela não autoriza ignorar falhas de decode.
Não registrar identificadores ou conteúdo dos dispositivos nas mensagens de diagnóstico.
