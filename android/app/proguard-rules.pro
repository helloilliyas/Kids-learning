# Keep kotlinx.serialization generated serializers for the lesson model.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.kidslearning.app.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.kidslearning.app.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
