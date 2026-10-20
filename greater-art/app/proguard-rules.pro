# AGP keeps manifest entry points (Application, activities, services and
# FileProvider). Media3, Compose and DataStore supply their consumer keep rules.
# Do not blanket-keep those libraries: that defeats shrinking and hides missing rules.

# Retain Kotlin metadata and line numbers for local crash-report/mapping analysis.
-keep class kotlin.Metadata { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,LineNumberTable,SourceFile

# Preference/backup enum identities are persisted by name. Keep their constants
# and standard lookup entry points stable while allowing method optimization.
-keepclassmembers,allowoptimization enum com.local.listentomusic.data.** {
    public static ** *;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
