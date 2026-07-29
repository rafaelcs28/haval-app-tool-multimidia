-keep class com.autolink.** { *; }
-keepnames class com.autolink.**
-keepclassmembers class com.autolink.** { *; }
-keepclassmembers enum com.autolink.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public *;
}
-keepclassmembers class com.autolink.** implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Preview/release builds must not emit diagnostic logcat output. The debug and internalDebug
# variants keep logs available for bench/central diagnostics.
-maximumremovedandroidloglevel 7
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** println(...);
    public static *** isLoggable(...);
    public static *** getStackTraceString(...);
}

# Ambient Light: enums e o modelo de regra sao PERSISTIDOS (prefs guardam enum.name; regras de
# automacao em JSON). Sem keep, o R8 renomeia constantes (ex.: NO_SEATBELT->e) e campos
# (condition->a) DIFERENTE a cada build -> o que um build salvou vira lixo no proximo (config
# reseta sozinha; regra desserializa com enum nulo e crasha UI/motor). Mantem os nomes estaveis.
-keepnames class br.com.redesurftank.havalshisuku.ambientlight.** { *; }

# Enums de models sao PERSISTIDOS por NOME em prefs (ex.: SteeringWheelAcControlType em
# LAST_CLUSTER_AC_CONFIG; SteeringWheel*ActionType/ClimateCommandType nos botoes do volante).
# Sem keep, o R8 renomeia as constantes DIFERENTE a cada build -> um valueOf() de valor salvo
# pelo build anterior estoura IllegalArgumentException (crash-loop no card de AC do cluster).
# Mesma armadilha documentada no ambientlight acima; manter os nomes das constantes estaveis.
-keepnames enum br.com.redesurftank.havalshisuku.models.** { *; }

# Classe-ESPELHO do OEM (ts.car.dcm.common.data.ConnDevice): existe no nosso fonte so pra
# desserializar o Parcelable que o servico DCM do carro devolve. O nome DEVE bater EXATAMENTE com o
# do OEM, e os metodos sao chamados por REFLEXAO (getDeviceUuid/getFriendlyName/getUsbSerialNumber/
# getBtAddr/getAvailableCapabilitys/getActiveCapability/getDeviceConnectedState em
# AndroidAutoDcmRecovery.invokeString/invokeInt).
# BUG QUE ISTO CORRIGE: sem keep, o R8 renomeava a classe (mapping: ConnDevice -> w10), entao
# Class.forName("ts.car.dcm.common.data.ConnDevice") lancava ClassNotFoundException, o
# bundle.classLoader nao era setado e o unmarshal do parcel do OEM falhava (logcat: "E Parcel ...
# Didn't find class ts.car.dcm.common.data.ConnDevice"). Como o codigo usa runCatching{}.getOrNull(),
# a falha era SILENCIOSA: getDevices() devolvia lista VAZIA em TODA build de release/preview -> a
# recuperacao DCM do Android Auto ficava inerte. Mesma familia do bug de enum renomeado acima.
# Espelha o tratamento que com.autolink.** (outra classe-espelho do OEM) ja recebe no topo.
-keep class ts.car.dcm.** { *; }
-keepnames class ts.car.dcm.** { *; }
