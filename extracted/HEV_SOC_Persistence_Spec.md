# Feature: Persistir o % de bateria do HEV Prioritário (Haval H6 PHEV)

## Problema que resolve
No modo **HEV Prioritário**, o carro **reseta sozinho** o % alvo de bateria (vai pra 20 ou 80)
ao religar ou após um tempo dirigindo. O usuário perde o valor que escolheu. Esta função
**reaplica automaticamente** o % escolhido — **SEM nunca mexer no modo de condução** (HEV/EV/
Prioridade EV), que continua 100% controlado pelo usuário.

## Chaves CAN (confirmadas no veículo — Haval H6 PHEV)
| Conceito | Chave (key) | Valores |
|---|---|---|
| Modo de condução | `car.ev_setting.power_model_config` | **0 = HEV**, 1 = EV Prioritário, 3 = EV |
| Sub-modo do HEV | `car.ev_setting.power_reserve_config` | 1 = Inteligente, **2 = Prioritário** |
| % alvo de bateria | `car.ev_setting.charge_soc_target_config` | inteiro **20–80** (o valor em %) |

## Mecanismo que o app precisa ter (dependência)
O app precisa falar com o serviço de propriedades do carro (no nosso caso, via Shizuku):
- **Ler:** `getData(key)` / `getUpdatedData(key)` → retorna `String` (leitura ao vivo).
- **Escrever:** `updateData(key, value)` — `value` é `String`.
- **Escutar mudanças:** um callback `onDataChanged(key, value)` que dispara quando uma
  propriedade do carro muda.
  > ⚠️ **CRÍTICO:** o app só recebe callback das chaves que ele **registra pra escutar**
  > (lista tipo `DEFAULT_KEYS`). Então `car.ev_setting.charge_soc_target_config` **e**
  > `car.ev_setting.power_reserve_config` PRECISAM estar nessa lista, senão os gatilhos
  > nunca disparam.

## Lógica central
Um método `applyHevSocTargetIfActive(reason)`:
1. Se a feature está desligada (pref) → não faz nada.
2. **Trava estrita:** só atua se `power_model_config == "0"` (HEV) **E**
   `power_reserve_config == "2"` (Prioritário). Caso contrário → `return` (não escreve NADA —
   é isso que garante que NUNCA mexe no modo).
3. Lê o `charge_soc_target_config` atual. Se for **diferente** do valor desejado (pref),
   **reescreve** o desejado.
4. O eco da própria escrita não causa loop: depois de escrever, `current == desired`, então
   o próximo disparo não reescreve.

Dispara em **3 momentos**:
- Quando `charge_soc_target_config` muda (o carro resetou o alvo) → `"SOC_CHANGED"`.
- Quando `power_reserve_config` vira `"2"` (usuário entrou em Prioritário) → `"ENTER_PRIORITARIO"`.
- No **power-on** (quando o estado de "driving ready" fica ativo) → `"POWER_ON"`.

## Código de referência (Java — adapte ao seu app)
```java
// Chamar nos 3 gatilhos. So reescreve o % alvo quando o carro JA esta em HEV Prioritario.
public void applyHevSocTargetIfActive(String reason) {
    try {
        if (!prefs.getBoolean("enablePersistHevSocTarget", false)) return;

        // 1) Trava estrita: HEV (0). Em EV / Prioridade EV, sai sem escrever nada.
        String driveMode = getUpdatedData("car.ev_setting.power_model_config");
        if (driveMode == null || !driveMode.trim().equals("0")) return;

        // 2) E sub-modo Prioritario (2).
        String subMode = getUpdatedData("car.ev_setting.power_reserve_config");
        if (subMode == null || !subMode.trim().equals("2")) return;

        int desired = prefs.getInt("hevSocTargetValue", 50);
        String currentStr = getUpdatedData("car.ev_setting.charge_soc_target_config");
        int current = Integer.MIN_VALUE;
        try { current = Integer.parseInt(currentStr.trim()); } catch (Exception ignored) {}

        if (current != desired) {
            updateData("car.ev_setting.charge_soc_target_config", String.valueOf(desired));
            // log: "[HEV-SOC " + reason + "] alvo estava " + current + ", reaplicado " + desired
        }
    } catch (Exception e) { /* log e.printStackTrace */ }
}
```

```java
// No handler de mudanca de propriedade do carro:
void onDataChanged(String key, String value) {
    // ... outras chaves do app ...
    if (key.equals("car.ev_setting.charge_soc_target_config")) {
        applyHevSocTargetIfActive("SOC_CHANGED");        // o carro mexeu no alvo -> reverte
    } else if (key.equals("car.ev_setting.power_reserve_config") && value.trim().equals("2")) {
        applyHevSocTargetIfActive("ENTER_PRIORITARIO");  // entrou em Prioritario -> aplica
    }
}
// E no power-on (quando o estado de driving-ready fica ativo): applyHevSocTargetIfActive("POWER_ON");
```

> Lembre de adicionar `car.ev_setting.charge_soc_target_config` e
> `car.ev_setting.power_reserve_config` na lista de chaves escutadas (DEFAULT_KEYS).

## Preferências (persistência local)
- `enablePersistHevSocTarget` — boolean, default `false` (liga/desliga a função).
- `hevSocTargetValue` — int, default `50`, faixa `20–80` (o % que o usuário escolheu).

## UI sugerida (Compose — exemplo)
```kotlin
// Toggle "Manter % de bateria no HEV Prioritário"
Switch(checked = enable, onCheckedChange = {
    enable = it
    prefs.putBoolean("enablePersistHevSocTarget", it)
    if (it) serviceManager.applyHevSocTargetIfActive("UI_TOGGLE")  // aplica na hora se ja estiver em HEV Prio
})

// Slider 20..80 (aparece quando o toggle esta ligado)
Slider(
    value = target.toFloat(),
    onValueChange = { target = it.toInt() },
    onValueChangeFinished = {
        prefs.putInt("hevSocTargetValue", target)
        serviceManager.applyHevSocTargetIfActive("UI_SLIDER")
    },
    valueRange = 20f..80f,
    steps = 11,           // 20,25,...,80 (passo 5)
)
```

## Cuidados (importantes)
1. **NUNCA escrever** `power_model_config` nem `power_reserve_config`. A única escrita é o
   `charge_soc_target_config`. O modo é do usuário.
2. A **trava estrita** (HEV 0 + Prioritário 2) é o que garante que a função fica 100% silenciosa
   fora do HEV Prioritário — sem isso, escrever o alvo em outro modo poderia (em tese) fazer o
   carro reagir.
3. **Validar no veículo** que a escrita "gruda" (que o carro aceita `updateData` nessa chave) —
   é a única incerteza de runtime. Teste: entra em HEV Prioritário, põe 50, muda pra 20 no menu
   nativo do carro → deve voltar pra 50 sozinho.
