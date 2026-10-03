# AFR CHAT - règles ProGuard/R8 pour le build release

# Modèles de données + DTO Supabase (kotlinx.serialization)
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.afrchat.app.data.model.** {
  <fields>;
  <init>();
}
-keep class com.afrchat.app.data.model.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Coroutines
-dontwarn kotlinx.coroutines.**

# Classes optionnelles référencées mais absentes (évite l'échec R8 en release)
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# supabase-kt / Ktor / kotlinx.serialization
-keep class com.afrchat.app.data.remote.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn io.ktor.**
-dontwarn kotlinx.serialization.**
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
