# قواعد ProGuard/R8 — تُستخدم عند تفعيل minifyEnabled مستقبلًا

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.nba.plus.**$$serializer { *; }
-keepclassmembers class com.nba.plus.** { *** Companion; }
-keepclasseswithmembers class com.nba.plus.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit
-keepattributes Signature, Exceptions
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
