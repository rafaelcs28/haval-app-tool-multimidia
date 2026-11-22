# Carimbo Impulse — spec de implementação (Opção B "Cápsulas" v2)

Mockup de referência: `optB.html` (renders em `out/optB-day.png`, `out/optB-night.png`, `out/optB-alert.png`).
Todas as medidas são para um quadro de **1920×1080**. Coordenadas absolutas (x da esquerda, y do topo).

## 0. Convenções

- **Escala:** `s = min(W/1920, H/1080)`. Multiplique por `s` todas as medidas (posições, tamanhos, raios, traços, fontes, letter-spacing).
  A cápsula esquerda fica ancorada no canto inferior esquerdo e a direita no canto inferior direito:
  `xL = 36·s`, `xR = W − 36·s − 800·s`, `yTop = H − 36·s − 268·s`. Todas as coordenadas internas abaixo são relativas a essas origens.
  - 2560×1440 → s = 1,3333 (cápsula L 853×357 px em (48, 1035); fonte da velocidade 186,7 px)
  - 1280×720 → s = 0,6667 (texto mínimo 17·s ≈ 11 px: ainda nítido no próprio vídeo)
  - 4:3 ou outros formatos: mesmo `s = min(...)`, com as cápsulas sempre ancoradas nos cantos inferiores.
- **Texto:** todos os `top` abaixo são o topo de uma caixa com line-height = 1. No Canvas,
  **baseline = top + 0,842·size** (métricas do Roboto: ascent 0,928, descent 0,244). Os baselines já calculados aparecem como `bl`.
- **Fontes:** Cond = `Typeface.create("sans-serif-condensed", …)` (Roboto Condensed). Mono = Roboto Mono embutida no app
  (assets/fonts/RobotoMono-Medium/Regular). O `Typeface.MONOSPACE` do sistema também funciona, mas as larguras mudam. Use `Paint.setFakeBoldText(false)` e o peso real.
  Algarismos tabulares: `paint.fontFeatureSettings = "tnum"`, para que velocidade, hora e kW não "dancem" de um quadro para o outro.
- **Sombra no texto:** não é necessária dentro das cápsulas. Só o selo EVENTO e os chips não levam sombra.
- **Cores** (ARGB):
  - branco `#FFFFFFFF`; branco secundário `#9EFFFFFF` (62%); terciário `#80FFFFFF` (50%); rótulos `#8CFFFFFF` (55%); inativo `#3DFFFFFF` (24%)
  - verde (setas, regeneração, farol baixo) `#FF3EE08F`; âmbar `#FFFFB020`; vermelho `#FFFF4A4A`; azul farol alto `#FF5AA9FF`; ciano trilha `#FF4FD1FF`

## 1. Cápsulas (fundo)

| | x | y | w | h | raio |
|---|---|---|---|---|---|
| Cápsula L (dinâmica e segurança) | 36 | 776 | 640 | 268 | 30 |
| Cápsula R (onde e quando) | 1084 | 776 | 800 | 268 | 30 |

- Preenchimento: `LinearGradient` vertical de `#CC1A1D24` (topo) até `#CC090B0E` (base), ou seja, alfa de 80%.
- Contorno: 1,5 px `#1FFFFFFF` (12%), desenhado por dentro (inset 0,75 px).
- Opcional: `setShadowLayer(24, 0, 6, #40000000)` no retângulo (exige aceleração por hardware na API 28+, ou um bitmap pré-renderizado).
- **Estado EVENTO** (clipe de emergência do G-sensor): contorno extra de 3 px `#E6FF4A4A` por dentro da cápsula L, mais o selo do item 4.

## 2. Cápsula L: primária (velocidade, aceleração/frenagem, fluxo de potência) e secundária (segurança)

Origem (36, 776). Valores absolutos.

### 2.1 Velocidade (primária)
- "48": Cond Bold **140 px**, letter-spacing −3 px, branco. x = 62, top 792 → **bl 910**. Alinhada à esquerda.
  Com 3 dígitos (≥ 100 km/h): **112 px**, top 816, mantendo o mesmo bl 910, para não invadir a placa.
  **Acima do limite**, a velocidade fica vermelha `#FFFF4A4A`. Regra de tolerância do CTB: excede quando `v > limite + 7` (limites até 100 km/h) ou `v > limite × 1,07` (limites acima de 100).
- **Placa de limite** (estilo R-19 brasileiro): círculo com centro em **(274, 916)** e r 28.
  - Fundo branco `#FFFFFFFF`, anel vermelho `#FFE02424` com 6 px de largura por dentro (desenhe com traço de 6 em r 25).
  - Número "60" em Cond Bold 24 px `#FF111111`, centralizado, com bl ≈ 924.
  - Some quando o limite da via é desconhecido.
- "KM/H": Cond Regular 24 px, letter-spacing 1 px, `#9EFFFFFF`. x = 66, top 924 → bl 944.

### 2.2 Aceleração/frenagem (primária)
- Rótulo: "ACELERAÇÃO" se g ≥ 0, "FRENAGEM" se g < 0. Cond Bold 17 px, letter-spacing 2,5 px, `#8CFFFFFF`. x = 322, top 806 → bl 820.
- Valor: "+0,12 g" / "−0,52 g" (sinal de menos U+2212, vírgula decimal). Cond Bold 38 px, **alinhado à direita em x = 652**, top 794 → bl 826.
  Cor: g > −0,30 → branco; −0,50 < g ≤ −0,30 → âmbar; g ≤ −0,50 → vermelho.
- Barra bidirecional: trilho x 322, y 842, 330×8, raio 4, `#29FFFFFF` (16%). Centro (0 g) em x = 487.
  Marca do zero: retângulo de 2×18 em (486, 837), raio 1, `#D9FFFFFF`.
  Preenchimento: largura = `min(|g|/0,8, 1)·164`. Vai do centro para a direita quando g ≥ 0 (branco) e para a esquerda quando g < 0 (com a cor do valor). Raio 4.

### 2.3 Fluxo de potência (primária)
- Rótulo: "ACELERANDO" (kW ≥ 0) ou "REGENERANDO" (kW < 0). Mesmo estilo do 2.2. x = 322, top 882 → bl 896.
- Valor: "42 kW · 31%" / "18 kW · 12%": Cond Bold 38 px, alinhado à direita em x = 652, top 870 → bl 902. O separador " · " usa Cond Regular com `#99FFFFFF`.
  Cor: acelerando = branco; regenerando = verde. O kW aparece sem sinal, porque o rótulo já indica o sentido. O % é a fração da potência disponível naquele sentido: tração em relação à potência máxima disponível, regeneração em relação ao limite de regeneração.
- Barra: igual à do 2.2, em y = 918 (trilho x 322, 330×8; zero em x = 487; marca do zero em y 913..931).
  Largura = `min(pct/100, 1)·164`, para a direita (branco) ao acelerar e para a esquerda (verde) ao regenerar.
- Com 0 kW (parado ou em ponto morto): rótulo "POTÊNCIA", valor "0 kW · 0%" em `#9EFFFFFF`, sem preenchimento.

### 2.4 Separador
- x 60 → 652, y 952, altura 1,5 px, `#1AFFFFFF`.

### 2.5 Linha de segurança (secundária)
Linha centrada em y = 982 (topo 964, altura 36). Da esquerda para a direita, a partir de x = 62, com 20 px entre os itens:

| item | tamanho | ativo | inativo |
|---|---|---|---|
| cinto | ícone 34 | OK: ícone branco | **sem cinto:** chip vermelho `#FFFF4A4A`, raio 9, padding 6/10, ícone branco 26 + "SEM CINTO" Cond Bold 19 px, letter-spacing 1 |
| seta esquerda | 34 | verde (pisca em 1 Hz de vídeo, se o sinal for amostrado; caso contrário, fixo) | `#3DFFFFFF` |
| seta direita | 34 | verde | inativo |
| pisca-alerta | 32 | triângulo vermelho, com as duas setas verdes | inativo |
| faróis | 34 | baixo: verde (ícone com raios inclinados); alto: azul `#5AA9FF` (preenchido, raios horizontais) | desligado: inativo |
| freio de mão | 34 | vermelho | inativo |
| marcha | chip branco, raio 8, padding 5/10; letra Cond Bold 26 px `#0C0E12` | sempre visível: P, R, N ou D | — |
| volante | ícone 32, girado pelo ângulo real (CW +) + "−12°" Cond Regular 26 px `#E0FFFFFF` | **alinhado à direita em x = 652** | — |

Com o chip SEM CINTO, os ícones seguintes deslocam-se para a direita. Os ícones são paths vetoriais sobre uma grade de 24 com traço de 1,9 (veja `ICON` em `common.js`), escalados para o tamanho indicado.

### 2.6 Motor, travas e porta (secundária/contexto)
Linha centrada em y = 1020 (topo 1008, altura 24):
- Ícone de motor 24 px em x = 62. Texto Cond Regular 22 px em x = 94, bl ≈ 1028.
  - Ligado: "Motor 1.450 rpm" (milhar com ponto, pt-BR) em `#E0FFFFFF`.
  - Desligado: "Motor desligado" em `#52FFFFFF` (32%).
- Travas: ícone de cadeado 22 px em x ≈ 276 + "Travado"/"Destravado" Cond Regular 22 px em x ≈ 304, cor `#8CFFFFFF`. É contexto discreto, logo depois do texto do motor (18 px de espaço). O cadeado aberto ou fechado acompanha o estado.
- **Porta aberta** (só quando ativa): chip vermelho alinhado à direita em x = 652, centro y 1020, altura ≈ 28, raio 9, com ícone de porta branco 20 + "PORTA ABERTA" Cond Bold 19 px, letter-spacing 1. Pode receber um sufixo com a porta, como "· TRASEIRA E".

### 2.7 Faixa de alertas acima da cápsula L (só quando houver alerta)
Linha de chips em x = 36, topo y = 716, altura 44, raio 22, 10 px entre os chips. Ícone 24 + texto Cond Bold 22 px, letter-spacing 1, padding 12 / 16. Ordem:
1. **"ACIMA DO LIMITE · 60"**: fundo `#F2FF4A4A`, texto e ícone de alerta brancos. Aparece enquanto vale a regra de tolerância do 2.1.
2. **"Radar a 240 m · 60"**: fundo `#D10C0E12`, contorno de 2 px âmbar `#FFFFB020` por dentro, texto e ícone de radar âmbar.
   - Só aparece quando o radar está a **800 m ou menos** à frente, na direção do rumo.
   - Distância arredondada para 10 m (abaixo de 1 km).
   - Some depois que o radar é ultrapassado.
Sem alertas, nada é desenhado. A faixa cobre no máximo x 36–~580, y 716–760, sobre o acostamento ou a lateral, e não sobre o centro da pista.

## 3. Cápsula R: contexto (onde, quando, carro, evidência)

Origem (1084, 776).

### 3.1 Mini-mapa
- Retângulo x 1100, y 792, 236×236, raio 20. Recorte com `clipPath(RoundRect)`.
- **Tiles OSM no zoom 16**, desenhados com escala de 0,85 px de saída por px de tile (a 1080p). Na latitude de Goiânia, 1 px de tile ≈ 2,29 m, então o mapa cobre cerca de 280 px de tile, ou **~640 m de largura**.
  Acima de 90 km/h, use zoom 15 (escala 0,85, ~1,3 km). Abaixo de 15 km/h, zoom 17 é opcional.
- **Norte para cima** (é o melhor para evidência). O carro fica centralizado: o deslocamento do tile vem da posição do carro em pixels mundiais do zoom 16.
- Estilo "mapa noturno": `ColorMatrixColorFilter` = brilho(0,78) · contraste(0,92) · saturação(0,45) · inverter. Isso equivale ao CSS `invert(1) hue-rotate(180deg) saturate(.45) brightness(.78) contrast(.92)`. A rotação de matiz fica dispensável, porque a saturação baixa quase anula o efeito.
  Matriz pronta (linhas R, G, B, A; offsets em 0–255), equivalente a inverter → saturação 0,45 → escala 0,72:
  ```
  sat 0.45: a=0.45; lr=0.213*(1-a)=0.1172 lg=0.715*(1-a)=0.3933 lb=0.072*(1-a)=0.0396
  M_sat = [lr+a lg lb 0 0 ; lr lg+a lb 0 0 ; lr lg lb+a 0 0 ; 0 0 0 1 0]
  M_inv = [-1 0 0 0 255 ; 0 -1 0 0 255 ; 0 0 -1 0 255 ; 0 0 0 1 0]
  M = scale(0.72) · M_sat · M_inv   (ColorMatrix.postConcat, nesta ordem)
  ```
- Trilha percorrida (posições já gravadas no clipe): duas polylines com cantos arredondados, primeiro uma faixa de brilho de 12 px `#474FD1FF` e por cima a linha de 4,7 px `#FF4FD1FF`.
- Trecho seguinte do clipe (opcional, já que o clipe salvo conhece o futuro): pontilhado de 4,4 px `#734FD1FF`, com dash [2, 7,6] e ponta redonda.
- Seta do carro no centro (1218, 910), girada pelo rumo:
  - halo: círculo r 25, `#2E4FD1FF`;
  - seta: path (0,−16) (12,13) (0,7) (−12,13), fechado, preenchimento branco e contorno 2,5 `#0B1016`.
- Vinheta interna (opcional): `RadialGradient` de transparente até `#73000000` na borda, mais um contorno interno de 1,5 px `#29FFFFFF`.
- Selo "N": círculo r 15 com centro em (1125, 817), preenchimento `#D9080A0E`, contorno 1,5 `#33FFFFFF`; "N" em Cond Bold 17 branco, centralizado.
- Chip de rumo: "ENE 73°" (rosa de 16 pontos em pt-BR: N, NNE, NE, ENE, L, ESE, SE, SSE, S, SSO, SO, OSO, O, ONO, NO, NNO). Pílula em x 1168, y 988, 100×30, raio 15, com o mesmo fundo do selo N; Cond Bold 20, letter-spacing 1, centralizado (bl ≈ 1010).

### 3.2 Quando e onde
- Hora "12:31:47": Mono Medium 38 px, branco. x 1360, top 802 → **bl 834**.
- Data "22/11/2026": Mono Regular 25 px, `#9EFFFFFF`. x 1564, bl 834 (mesmo baseline da hora).
- "IMPULSE": Cond Bold 15 px, letter-spacing 5, `#66FFFFFF`, alinhado à direita em x 1858, bl 825.
- Logradouro "Av. Anhanguera, 1450": Cond Bold 44 px, branco. x 1359, top 854 → **bl 891**. Largura máxima de 499 px, truncado com "…" (`TextUtils.ellipsize`).
- Bairro/cidade "Setor Central, Goiânia": Cond Regular 27 px, `#BFFFFFFF`. x 1360, bl 929. Largura máxima até x 1610.
- Coordenadas "−16.67460, −49.25620": Mono Regular 19 px, `#80FFFFFF`, alinhadas à direita em x 1858, bl 928. Ponto decimal, que é o padrão de coordenadas.

### 3.3 Barra de contexto (terciária)
- Fundo: x 1354, y 948, 514×44, raio 14, `#12FFFFFF` (7%).
- 4 itens distribuídos (space-between, padding horizontal 16), centro y 970. Entre eles, divisores verticais de 1,5×22 `#24FFFFFF`.
  - Modo: "Normal" / "Eco" / "Sport".
  - Bateria (SOC): ícone 22 + "72%".
  - Temperatura: ícone 22 + "23° int · 31° ext".
  - Hodômetro: ícone 22 + "12.483 km".
- Texto em Cond Regular 24 px, `#E0FFFFFF`, ícones com 6 px até o texto.

### 3.4 Linha de evidência (terciária, discreta)
- "Original SHA-256 a3f9c2e81b04 · 0,8 km no minuto": Mono Regular 17 px, `#6BFFFFFF` (42%). x 1362, top 1006 → bl 1020.
  - O hash são os 12 primeiros caracteres hex, em minúsculas, do SHA-256 do arquivo original da câmera.
  - O km é a distância percorrida no minuto do relógio atual, com 1 casa decimal e vírgula.

## 4. Selo EVENTO (só quando a câmera marcou o clipe como emergência / G-sensor)
- Pílula vermelha `#EBDC2626`, alinhada à direita em x 1884, y 32, altura 46, raio 23, padding 16 / 20.
- Ponto branco r 6 + "EVENTO · FRENAGEM BRUSCA" (ou só "EVENTO", se a causa for desconhecida): Cond Bold 26, letter-spacing 3, branco.
- Fica no canto superior direito. O canto superior esquerdo pertence ao carimbo de data/hora da própria câmera e não deve receber nada.

## 5. O que esconder ou atenuar quando inativo
- Setas, pisca-alerta, faróis desligados e freio de mão solto: desenhados com 24% de alfa. Continuam visíveis, para mostrar que o dado existe e estava desligado.
- Cinto OK: ícone branco simples. Sem cinto: chip vermelho.
- Porta aberta, ACIMA DO LIMITE, Radar e EVENTO: **não são desenhados** quando inativos.
- Placa de limite: some quando o limite é desconhecido.
- Travas: sempre presentes, em tom discreto.
- Motor desligado: texto com 32% de alfa.
- Dados ausentes (sem GPS, sem CAN):
  - substitua o valor por "—" em `#66FFFFFF`;
  - sem fix de GPS: mapa com tiles atenuados e sem seta, e logradouro "Sem sinal de GPS".

## 6. Hierarquia e legibilidade
- Primária: velocidade 140, g e potência 38 (3 a 4 vezes o tamanho do contexto).
- Secundária: ícones 32–34 e motor 22.
- Terciária: contexto 24, coordenadas 19, evidência 17.
- Num celular (vídeo reduzido ~3×), velocidade, g, potência, hora e logradouro continuam legíveis (veja `out/*-640.png`). As coordenadas e o hash são deliberadamente pequenos: servem para pausar e ampliar.
- Área ocupada: 2 cápsulas, cerca de 1440×268 px, deixando livre o centro da faixa inferior (x 676–1084) e toda a pista acima de y 776.

## 7. Desempenho (Canvas → re-encode)
- Pré-renderize uma vez por clipe: os fundos das cápsulas (bitmap ARGB) e os ícones (Picture/Path).
- Por quadro, só texto, barras, ícones de estado e mapa.
- Mapa: redesenhe quando a posição mudar (1 Hz), com os tiles em cache em LRU já filtrados pela ColorMatrix, e reutilize o bitmap entre os quadros do mesmo segundo. Interpolar a posição do carro entre amostras de 1 Hz deixa o movimento suave.
- Valores numéricos atualizam a 1 Hz, conforme os dados. A barra de g pode ser interpolada.
