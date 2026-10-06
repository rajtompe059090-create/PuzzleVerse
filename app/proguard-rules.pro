# ProGuard / R8 Rules for PuzzleVerse

# Preserve Room database entities and DAOs
-keep class androidx.room.** { *; }
-keep class com.example.data.entity.** { *; }
-keep class com.example.data.dao.** { *; }
-dontwarn androidx.room.paging.**

# Google Mobile Ads (AdMob)
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}
-dontwarn com.google.android.gms.ads.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Keep line numbers for crash analysis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
