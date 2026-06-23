# Handoff — Fallback WiFi↔4G no EcoTrip (celular)

Como o **EcoTrip do celular** deve escolher entre as duas fontes de dados do carro e cair pra fonte certa quando o 4G congelar. Autocontido.

## Arquitetura (confirmada)
- **EcoTrip head unit** = publisher: lê o CAN e publica `haval/ecotrip/*` no broker (via WiFi). Assina os tópicos de comando (`cmd/*`, `ha/*/set`).
- **EcoTrip celular** = consumidor + comandos: exibe os dados e publica comandos.
- **Broker:** `mqttrafael.duckdns.org:8883` (TLS, Let's Encrypt), user `rafael_haval`.
- Duas fontes de leitura no celular: **gwmbrasil** (4G, nuvem GWM) e **`haval/ecotrip/*`** (WiFi, do head unit).

## O problema do frescor
- **gwmbrasil (4G):** NÃO expõe carimbo de frescor; a entidade segura o último estado pra sempre. Quando o 4G acaba, o TBOX para de alimentar a nuvem → a gwmbrasil devolve dado **congelado** sem avisar. → impossível detectar "stale" olhando só pra ela.
- **"Nenhuma chave mudou" NÃO é sinal de stale:** carro parado fica horas sem mudar nada — estado válido.

## A regra (vivacidade do EcoTrip como chave)
O EcoTrip head unit está vivo **exatamente quando o congelamento do 4G importaria** (carro ligado/carregando). Quando o carro dorme, o EcoTrip cai (`status=offline`) — mas aí nada muda, então o valor congelado do 4G está correto. Então **não precisa detectar o 4G**:

```
ecotrip_vivo = (agora − haval/ecotrip/last_update < HEARTBEAT_TTL)   # ~90s — SINAL PRIMÁRIO
# NÃO use o status como gatilho: o EcoTrip publica em rajadas e fica "offline" entre elas

se ecotrip_vivo:
    fonte = haval/ecotrip/*        # tempo real, direto do CAN; vale mesmo com 4G morto
senão:
    fonte = gwmbrasil (4G)         # carro dormindo → último valor é o correto
    # opcional: flag "frescor desconhecido" (gwmbrasil não dá timestamp)
```

### Sinais de vivacidade (já existem no EcoTrip)
- `haval/ecotrip/last_update` → timestamp; **pinga a cada ciclo quando o carro está ligado/carregando** (heartbeat real). Para quando dorme. **← SINAL PRIMÁRIO: use a idade dele.**
- `haval/ecotrip/status` → `online`/`offline`. ⚠️ Na prática o EcoTrip conecta/publica em **rajadas** e fica `offline` entre elas (confirmado: `status=offline` com `last_update` recente). Serve só como dica — **não** como disponibilidade contínua.
- `HEARTBEAT_TTL`: use ~2–3× o intervalo de publicação do head unit (ex.: publica a cada 30s → TTL 90s). Ajuste fino seu.
- *(Opcional, mais robusto: um tópico dedicado `haval/ecotrip/heartbeat` que SEMPRE pinga enquanto o head unit estiver acordado — assim a vivacidade não depende de o `last_update` ser tocado por outras lógicas.)*

## Pseudo-código (EcoTrip celular)
```
on_mqtt_message(topic, payload):
    if topic.startswith("haval/ecotrip/"):
        wifi[topic] = (payload, now())
    # gwmbrasil continua vindo pelo caminho de hoje (HA/nuvem)

def valor(campo):                       # campo = "soc_pct", "lock_state", ...
    lu   = parse_ts(wifi.get("haval/ecotrip/last_update"))
    vivo = (now() - lu < HEARTBEAT_TTL)     # PRIMÁRIO: idade do last_update (status é rajada → ignore)
    if vivo and ("haval/ecotrip/" + campo) in wifi:
        return wifi["haval/ecotrip/" + campo].value      # WiFi (real-time)
    return gwmbrasil_value(campo_equivalente_4g)         # 4G (último conhecido)

def fonte_atual():                      # pra mostrar um selo "WiFi"/"4G"/"sem dados" na UI
    if vivo:                 return "WiFi (ao vivo)"
    elif gwmbrasil_ok():     return "4G (último conhecido)"
    else:                    return "Sem dados — desatualizado"
```

## Comandos (phone → head unit)
Só funcionam com `haval/ecotrip/status == "online"` (head unit acordado e assinando). Se offline: desabilite os botões ou avise "carro dormindo — comando indisponível". (Comando remoto com o carro dormindo é justamente o que o TBOX 4G faria; nossa via exige o head unit acordado.)

## Mapa de campos (gwmbrasil 4G ↔ haval/ecotrip WiFi)
Referência (você conhece o schema do EcoTrip; segue o equivalente do lado 4G):

| Dado | gwmbrasil (4G) `…lgwffva55sh931315_…` | haval/ecotrip/… (WiFi) |
|---|---|---|
| SoC % | `estado_de_carga_soc` | `soc_pct` |
| Autonomia EV | `autonomia_ev` | `range_ev_km` / `ev_remain_km` |
| Autonomia comb. | `autonomia_combustao` | `fuel_remain_km` |
| Combustível | `nivel_de_combustivel` | `fuel_l` |
| Hodômetro | `quilometragem_total` | `odometer_km` |
| Carregando / tempo | `estado_da_carga` / `tempo_de_carga` | `charging_state` / `charge_remaining_min` |
| 12V | `estado_de_carga_12v` | `basic_battery_voltage_v` / `batt_12v_pct` |
| Trava | `estado_da_trava` | `lock_state` |
| Portas | `porta_*` | `door_fl/fr/rl/rr/trunk` |
| Vidros | `vidro_*` | `window_fl/fr/rl/rr` |
| Pneus pressão | `pressao_do_pneu_*` | `tyre_pressure_fl/fr/rl/rr` |
| Pneus temp | `temperatura_do_pneu_*` | `tyre_temp_fl/fr/rl/rr` |
| AC | `estado_do_ar_condicionado` | `ac_state` / `hvac_power_mode` |
| Desembaçadores | `estado_do_desembacador_*` | `hvac_front_defrost` / `hvac_rear_defrost` |
| Teto solar | `posicao_do_teto_solar` | `sunroof` |
| Localização | `endereco_atual` | `gps_lat` / `gps_lng` |

> Lacunas conhecidas no lado WiFi (se precisar, dá pra o head unit publicar): temperatura **da cabine** (gwmbrasil tem; não vi no `ecotrip/*`), aquecimento de bancos. Pneus/portas/vidros/AC já vêm **decodificados** no EcoTrip.

## Resumo
1. `ecotrip_vivo` = **idade do `last_update` < TTL** (status é rajada — não use como sinal contínuo).
2. Vivo → use `haval/ecotrip/*` (ao vivo, ignora 4G). Senão → gwmbrasil (último valor, correto pq carro dorme).
3. Nunca compare valores nem use "mudou/não mudou"; decida pela **vivacidade**, não pelo dado.
4. Comandos exigem `status online`.
