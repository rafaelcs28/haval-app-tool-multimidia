# Preparo CarPlay local — versões 133 a 137

A versão 133 inclui a biblioteca ARM64, o interceptor frio e o agente Java ESM
como assets verificados do APK. O Mac Mini compila a biblioteca e monta o pacote
em cada build imutável. Não há npm, Python, download ou controlador VPS na central.

`CarPlayBridgeBootstrapManager` inicia no processo principal do app por boot,
substituição do APK, abertura do app e disponibilidade do Shizuku. A opção é
persistida no armazenamento protegido do dispositivo; vem desativada por padrão.
O receiver de configuração exige a permissão DUMP e um boolean explícito.

Os payloads ficam em diretórios imutáveis por revisão. Os estados de execução
ficam separados por boot UUID, PID OEM e starttime do kernel. Dois injectors com
nome dedicado, desligados do terminal e da vida da Activity, mantêm os callbacks
nativo e Java vivos. Atualizar o APK não sobrescreve a biblioteca de uma sessão
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
