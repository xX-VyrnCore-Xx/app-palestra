# Add project specific ProGuard rules here.

# kotlinx.serialization resolves KSerializer for our @Serializable DTOs via reflection
# (used by Supabase-kt's decodeSingle/decodeList/upsert). Keep the DTOs and their
# generated serializers so R8 doesn't rename/strip what that lookup needs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.vyrncore.palestra.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.vyrncore.palestra.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.vyrncore.palestra.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vyrncore.palestra.data.remote.dto.** { *; }
