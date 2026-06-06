# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Line Numbers for Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room Persistence Library ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.robinzon.medicationwizard.database.** { *; }

# --- Project Entities & Models ---
# Keeping these ensures that SharedPreferences/JSON serialization doesn't break
-keep class com.robinzon.medicationwizard.entities.** { *; }

# --- Google Play Services / AdMob ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }

# --- WorkManager ---
-keep class androidx.work.** { *; }

# --- Google API Client & Google Drive ---
# These libraries use heavy reflection and runtime metadata
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.services.drive.model.** { *; }
-keep class com.google.common.util.concurrent.** { *; }
-keep class com.google.j2objc.annotations.** { *; }
-keep class com.google.errorprone.annotations.** { *; }

# --- gRPC (used by Firebase) ---
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class io.grpc.internal.** { *; }
-keep interface io.grpc.** { *; }

-dontwarn io.grpc.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**

-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn javax.lang.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.errorprone.annotations.**

# --- Glide ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# --- Play Review & Billing ---
-keep class com.google.android.play.core.review.** { *; }
-keep class com.android.billingclient.** { *; }

# General safety for View Binding
-keep class com.robinzon.medicationwizard.databinding.** { *; }
