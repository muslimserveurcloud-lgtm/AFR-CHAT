# AFR CHAT - règles ProGuard/R8 pour le build release

# Firebase / Firestore : conserve les modèles de données pour la (dé)sérialisation automatique
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
