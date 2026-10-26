# Preparo CarPlay local — versões 133 a 139

A versão 133 inclui a biblioteca ARM64, o interceptor frio e o agente Java ESM
como assets verificados do APK. O Mac Mini compila a biblioteca e monta o pacote
em cada build imutável. Não há npm, Python, download ou controlador VPS na central.

`CarPlayBridgeBootstrapManager` inicia no processo principal do app por boot,
substituição do APK, abertura do app e disponibilidade do Shizuku. A opção é
persistida no armazenamento protegido do dispositivo; vem desativada por padrão.
O receiver de configuração exige a permissão DUMP e um boolean explícito.

Os payloads ficam em diretórios imutáveis por revisão. Os estados de execução
ficam separados por boot UUID, PID OEM e starttime do kernel. Até a 137, dois injectors com nome dedicado mantinham os callbacks nativo e Java
vivos. Na 138, os agentes permanecem no processo OEM após o encerramento dos
clientes de injeção. Atualizar o APK não sobrescreve a biblioteca de uma sessão
existente. Não matar, descarregar ou substituir callbacks publicados.

A partir da versão 137, a preparação não consulta câmbio nem velocidade e não
depende de P ou de 0 km/h. A remoção foi solicitada explicitamente pelo proprietário
e cobre decisão inicial, publicação de elegibilidade, conferência anterior ao
reinício do host ocioso e atualização dos clientes. O pacote local de elegibilidade
não contém mais os campos de câmbio e velocidade; a idade da observação continua
limitando as evidências dos serviços de projeção.

A preparação exige ausência de tasks visuais CarPlay/AA e respostas DCM recentes
e sustentadas sem dispositivos
online nem projeções ativas. Desconexão apenas visual não basta. Binder indisponível,
resposta nula/malformada, processo ambíguo e dados antigos deixam o preparo pendente.
O DCM não expõe um sinal comprovado de inicialização concluída; aplicam-se espera
após bind e uma janela de amostras vazias, sem afirmar prova formal de prontidão OEM.

A preparação usa o gate frio revisado, limitado a dez segundos. Se a janela já
passou, pode renovar uma única vez o host ocioso, mantendo as mesmas barreiras.
O interceptor nunca instala hooks em bibliotecas OEM já carregadas. Antes de
publicar hooks, relê um arquivo de elegibilidade recente, ligado ao boot, PID,
starttime e tentativa. Mudança ou expiração aborta a preparação. A perda do
controlador não pode deixar a inicialização OEM presa indefinidamente.

Na versão 134, a publicação de elegibilidade e a revogação compartilham um lock. Uma amostra
concluída depois da troca de identidade ou do encerramento da tentativa não pode
reautorizar o ticket antigo. O diagnóstico de falha fica separado do arquivo
renovado pelo publisher. Na renovação do host, todas as condições vêm de uma única
versão do pacote, lida após as verificações de tasks e processo e conferida antes
do sinal. A revisão também cobre o código do controlador local; uma correção nele
não reutiliza silenciosamente a revisão cujo circuito de falha está aberto.

Falha abre um circuito por revisão; não há repetição automática ilimitada nem
remoção de hooks em estado incerto. Reinício do app reconhece os injectors e
arquivos da identidade viva. Um novo boot invalida os arquivos do processo velho.
A tela de teste mostra o estado e permite tentar novamente após inspeção.

A preparação arma somente o passthrough e o endpoint de controle. Não persiste
lease de teste, Surface Binder, decoder, envio type111, vídeo no display 3 ou opção
de mapa. O teste de cinco minutos continua sendo iniciado pelo usuário. Não altera
resolução, layout, WebView, tema escolhido, Android Auto ou controles do veículo.

O teste já não tinha bloqueio de câmbio ou velocidade. A versão 137 modifica apenas
as condições do preparo da multimídia. Um telefone conectado ou um estado de
projeção desconhecido continua impedindo uma nova preparação.

Se o telefone conectar antes das condições de preparação, o CarPlay original
continua disponível e o preparo aguarda a desconexão. A versão não promete que um
primeiro autoconnect seja interceptado em todos os boots. Validação de cold boot
real e mapa limpo continua separada da compilação e dos testes de política.


## Residência no processo OEM — 138

Após um reinício manual da 137, o agente nativo ficou em `armed` com a biblioteca
mapeada, mas seu cliente externo já não estava presente. O recibo antigo de preparo
não descrevia esse novo boot. A causa exata da saída do cliente não foi comprovada.
A versão 138 elimina a dependência da vida desse processo auxiliar: usa o modo
`-e` do mesmo executável Frida 17.5.2 já empacotado. O mecanismo mantém o script
no processo alvo após o cliente encerrar, conforme o [CLI oficial](https://github.com/frida/frida-core/blob/17.5.2/inject/inject.vala).

Um supervisor separado registra uma única execução, seu PID/starttime, o hash dos
argumentos completos e a saída. Não relança o cliente. Código zero sozinho não
comprova residência: somente depois da saída validada, o app cria um desafio novo.
O agente precisa responder de dentro do OEM com duas amostras sequenciais recentes,
ligadas a boot, PID/starttime, revisão, hash do script, ticket, papel e instância.
As respostas e os recibos são publicados por substituição atômica, em arquivos
separados por papel. Nenhuma evidência de outro boot ou outra tentativa é adotada.

O `dispose()` de cada agente invalida sua prova permanentemente. Isso distingue
um script residente de um unload comum que continua pendente por causa de um pin;
o [script engine oficial](https://github.com/frida/frida-core/blob/17.5.2/lib/payload/script-engine.vala)
chama `dispose` antes desse unload. O pin nativo permanece; o wrapper Java adquire
seu pin antes de publicar callbacks. Falha, dispose ou saída do cliente não soltam
pins nem revertem callbacks já publicados.

A inicialização OEM só é liberada após prova residente nativa em `armed` ou de
uma instalação dos nove hooks já concluída na janela fria. Isso também cobre o
carregamento natural anterior à confirmação do gate. O estado pronto exige prova nova de nove replacements nativos pinados, sem falha, e agente
Java armado. Reiniciar apenas o app repete o desafio para os agentes da mesma
identidade; ausência de cliente externo passa a ser esperada. Mapeamento legado,
prova ausente ou estado desconhecido continuam preservados, sem reinjeção.

A validação em Linux com o Frida 17.5.2 real comprovou respostas a desafios criados
após o cliente sair e execução de NativeCallback e trampoline `replaceFast`
residentes. O caso negativo de detach comum com pin invalidou a prova e rejeitou
outro desafio. Esses testes não executam Java/JNI ou os hooks OEM do Android.
O build e a validação de reinício, comandos Java e mapa na central permanecem
etapas separadas; a 137 instalada não é convertida por uma edição de arquivo.


## Registro de nascimento antes do exec — 139

Na primeira tentativa da 138 no veículo, o cliente retornou código zero e o agente
registrou `armed`, mas faltou `native-launch.json`; o recibo final tinha starttime
zero. A confirmação foi recusada. A leitura do argv do cliente enquanto ele ainda
estava vivo não produziu a evidência esperada. O motivo exato da janela perdida
não foi demonstrado; a correção remove essa dependência temporal.

O filho agora espera numa barreira antes de executar o CLI. O supervisor registra
PID e starttime e publica o recibo atômico com dono root e modo 400. Só depois cria
o arquivo exclusivo que libera o `exec`. O `exec` conserva a identidade capturada,
mesmo que o CLI termine imediatamente. A saída continua vindo do `wait` desse filho.

O hash dos argumentos passa a ser explicitamente declarado, com
`commandEvidence=declared-before-exec-v1`, e vinculado ao hash real do supervisor,
comparado com o programa gerado esperado. O script da função também é verificado
antes do fork. O injector de 52 MiB já é validado por `installPayload` antes da
janela fria; não se repete essa leitura dentro do gate. Um recibo ausente ou de
outro programa continua sendo recusado.

A barreira não substitui a prova posterior de residência: desafio novo após saída,
duas amostras frescas, dispose irreversível, nove hooks pinados e Java armado seguem
obrigatórios. Fixtures de shell comprovaram o caso de saída imediata e um atraso
artificial do supervisor; falhas de hash e de gravação impedem o exec. O preparo
real da versão 139 após boot continua dependendo da validação na central.

## 2026-10-26 — Recibos capturados durante a inicialização

Na141, uma falha de prazo4s preservou quiescência/elegibilidade positivas; o
recibo de saída0 tinha tempo36310ms anterior à falha36706ms. O código marcava
recibos pendentes antes da leitura e podia conservar a marca mesmo quando a
saída chegava durante a verificação. A ordem exata dentro da leitura de campo
não foi registrada.

A correção142 decide pendência usando os próprios recibos capturados após
validar dono, modo, tamanho, supervisor e identidade. Recibos que chegam durante
a conferência podem ser usados nessa leitura quando ambos foram validados.
Os prazos4s/15s/330s e as provas de residência posteriores à saída permanecem
obrigatórios; não há reinjeção, relaxamento de quiescência ou novo bloqueio deP.
31 fixtures locais de ciclo de vida/recibos, suíte JVM completa e Release
assinada passaram; build somente no Mini, commit `c4ca6523`. A 142 está publicada
e verificada no atualizador. Boot real continua separado e pendente.
