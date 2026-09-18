# Add project specific ProGuard rules here.

# kotlinx.serialization resolves KSerializer for our @Serializable DTOs via reflection
# (used by Supabase-kt's decodeSingle/decodeList/upsert). Keep every @Serializable class in the
# app and its generated serializer, wherever it lives - our DTOs aren't confined to one package
# (data.remote.dto, data.repository.*, data.sync.*, ...), and a class missed here doesn't fail
# to compile, it crashes at runtime the first time R8 has stripped its serializer.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.vyrncore.palestra.**$$serializer { *; }
-keepclassmembers class com.vyrncore.palestra.** {
    *** Companion;
}
-keepclasseswithmembers class com.vyrncore.palestra.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.vyrncore.palestra.**
-keep,includedescriptorclasses class <1> { *; }
