# Preserve Room Database & Entity metadata for R8 release shrinking
-keepattributes *Annotation*
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-dontwarn org.xmlpull.**

# Preserve Kotlin coroutines & Compose runtime essentials
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
