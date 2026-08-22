# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/polurival/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Compose specific rules
-keepclassmembers class  * {
    @androidx.compose.runtime.Composable *;
}

# Ktor rules
-keep class io.ktor.** { *; }
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Koin rules
-keep class org.koin.** { *; }

# kotlinx.serialization rules
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepnames class kotlinx.serialization.internal.GeneratedSerializer* {
    <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
