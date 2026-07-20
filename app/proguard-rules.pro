# Add project specific ProGuard rules here.
-keep public class * {
    public protected *;
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
