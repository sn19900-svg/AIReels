# قواعد ProGuard الافتراضية - يمكن تخصيصها لاحقاً حسب الحاجة
-keep class com.nabil.aireels.** { *; }
-dontwarn com.arthenica.**
-keep class com.arthenica.** { *; }
-keep class org.tensorflow.** { *; }
-keep class com.nabil.aireels.data.remote.gemini.** { *; }
-keep class com.nabil.aireels.domain.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
