-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.coroutines.**
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class kotlin.Metadata { *; }
