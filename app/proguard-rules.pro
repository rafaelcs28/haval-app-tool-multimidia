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
