# ProGuard rules for FS Media Player

# Media3 / ExoPlayer rules
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep codecs and decoders
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.decoder.** { *; }

# Coil
-keep class coil.** { *; }

# Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
