# ===================================================================
# Crashlytics & Stack Traces
# ===================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===================================================================
# General Attributes & Annotations
# ===================================================================
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ===================================================================
# Kotlinx Serialization
# ===================================================================
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    *;
}
-keepclassmembers class * {
    kotlinx.serialization.descriptors.SerialDescriptor getDescriptor();
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** $serializer;
    void write$Self(...);
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ===================================================================
# App Data Models, API DTOs & Persisted Schemas
# Keep models and their fields to guarantee JSON parsing, API calls,
# and DataStore caching never lose properties.
# ===================================================================
-keep class com.semavia.ailearn.data.** { *; }
-keepclassmembers class com.semavia.ailearn.data.** { *; }
-keep class com.semavia.ailearn.ui.plan.PlanModels** { *; }
-keepclassmembers class com.semavia.ailearn.ui.plan.PlanModels** { *; }
-keep class com.semavia.ailearn.ui.plan.PlanFlow** { *; }
-keepclassmembers class com.semavia.ailearn.ui.plan.PlanFlow** { *; }
-keep class com.semavia.ailearn.ui.onboarding.OnboardingModels** { *; }
-keepclassmembers class com.semavia.ailearn.ui.onboarding.OnboardingModels** { *; }
-keep class com.semavia.ailearn.ui.grammar.GrammarQuestion** { *; }
-keepclassmembers class com.semavia.ailearn.ui.grammar.GrammarQuestion** { *; }
-keep class com.semavia.ailearn.ui.grammar.OptionState { *; }
-keep class com.semavia.ailearn.ui.vocab.VocabWord** { *; }
-keepclassmembers class com.semavia.ailearn.ui.vocab.VocabWord** { *; }
-keep class com.semavia.ailearn.ui.tutor.ChatMessage** { *; }
-keepclassmembers class com.semavia.ailearn.ui.tutor.ChatMessage** { *; }
-keep class com.semavia.ailearn.ui.tutor.TutorAi** { *; }
-keepclassmembers class com.semavia.ailearn.ui.tutor.TutorAi** { *; }
-keep class com.semavia.ailearn.ui.speaking.SpeakingCard** { *; }
-keepclassmembers class com.semavia.ailearn.ui.speaking.SpeakingCard** { *; }
-keep class com.semavia.ailearn.BaseLanguage** { *; }
-keep class com.semavia.ailearn.BaseAppRepository** { *; }

# ===================================================================
# AndroidX Room & SQLite
# Preserves Room database, entities, DAOs, converters, and migrations
# ===================================================================
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
    @androidx.room.ProvidedTypeConverter *;
}
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# ===================================================================
# Jetpack Compose & Compose Multiplatform Resources
# ===================================================================
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class ailearn.shared.generated.resources.** { *; }
-keepclassmembers class ailearn.shared.generated.resources.** { *; }

# ===================================================================
# Firebase (Database, Remote Config, Analytics, Crashlytics)
# ===================================================================
-keepclassmembers class * {
    @com.google.firebase.database.IgnoreExtraProperties *;
    @com.google.firebase.database.PropertyName *;
    @com.google.firebase.database.Exclude *;
}
-keepclassmembers class * {
    <init>();
}
-dontwarn com.google.firebase.**

# ===================================================================
# Prox Ads, Custom Layouts & Mediation SDKs
# ===================================================================
-keep class com.proxglobal.ads.** { *; }
-keepclassmembers class com.proxglobal.ads.** { *; }
-keep class com.google.ads.pro.** { *; }
-keepclassmembers class com.google.ads.pro.** { *; }
-keep class com.semavia.ailearn.MyAdsUtils** { *; }
-keepclassmembers class com.semavia.ailearn.MyAdsUtils** { *; }
-keep class com.semavia.ailearn.CollapsePassThroughFrameLayout { *; }
-keepclassmembers class com.semavia.ailearn.CollapsePassThroughFrameLayout { *; }

# AdMob & Mediation Adapters
-keep class com.google.ads.mediation.** { *; }
-keep class com.google.android.gms.ads.** { *; }
-keepclassmembers class com.google.android.gms.ads.** { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.ironsource.** { *; }
-keep class com.mbridge.msdk.** { *; }
-keep class com.applovin.** { *; }
-keep class com.unity3d.services.** { *; }
-keep class com.vungle.** { *; }

# Suppress known optional dependencies warnings from mediation / tracking SDKs
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.bytedance.sdk.openadsdk.**
-dontwarn com.appsflyer.**
-keep class com.appsflyer.** { *; }

# ===================================================================
# Network & OkHttp / TLS Platform Providers
# ===================================================================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okio.**

# ===================================================================
# Kotlin Coroutines
# ===================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===================================================================
# Hilt & Dependency Injection
# ===================================================================
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject *;
    @javax.inject.Singleton *;
}

# ===================================================================
# AndroidX Media3 / ExoPlayer & Lottie
# ===================================================================
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-dontwarn androidx.media3.**

-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ===================================================================
# AndroidX DataStore Preferences
# ===================================================================
-keep class androidx.datastore.preferences.protobuf.** { *; }
-dontwarn androidx.datastore.**

# ===================================================================
# Android Platform Speech & TTS
# ===================================================================
-keep class com.semavia.ailearn.ui.speaking.PlatformTextToSpeech** { *; }
-keepclassmembers class com.semavia.ailearn.ui.speaking.PlatformTextToSpeech** { *; }
-keep class com.semavia.ailearn.ui.speaking.PlatformSpeechRecognition** { *; }
-keepclassmembers class com.semavia.ailearn.ui.speaking.PlatformSpeechRecognition** { *; }
