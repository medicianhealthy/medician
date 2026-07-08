# --- General Android & Project Safety ---
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# --- Project Entities & Database Models ---
# Keep all entities and database related classes to ensure Room and JSON serialization work
-keep class com.robinzon.medicationwizard.entities.** { *; }
-keep class com.robinzon.medicationwizard.database.** { *; }
-keep class com.robinzon.medicationwizard.ui.todaysmedications.DoseItem { *; }

# --- Room Persistence Library ---
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase {
    protected <methods>;
}
-dontwarn androidx.room.paging.**

# --- Google Play Services / AdMob / UMP ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**

# --- Google Sign-In & Auth ---
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.api.GoogleApiClient { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Google API Client & Google Drive ---
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-keep class com.google.j2objc.annotations.** { *; }
-keep class com.google.errorprone.annotations.** { *; }

-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.errorprone.annotations.**

# --- gRPC & Protobuf (Firebase compatibility) ---
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class io.grpc.internal.** { *; }
-keep interface io.grpc.** { *; }

-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**

# --- WorkManager ---
-keep class androidx.work.** { *; }

# --- Glide ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# --- Play Review & Billing ---
-keep class com.google.android.play.core.review.** { *; }
-keep class com.android.billingclient.** { *; }

# --- View Binding & Material Components ---
-keep class com.robinzon.medicationwizard.databinding.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.internal.CheckableImageButton

# --- Retrofit / OkHttp (Common dependencies) ---
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
