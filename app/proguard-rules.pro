# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep Line Numbers for Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Room Persistence Library ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.robinzon.medicationwizard.database.** { *; }

# --- Project Entities & Models ---
# Keeping these ensures that SharedPreferences/JSON serialization doesn't break
-keep class com.robinzon.medicationwizard.entities.** { *; }
-keep class com.robinzon.medicationwizard.database.** { *; }

# --- Google Play Services / AdMob ---
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }

# --- WorkManager ---
-keep class androidx.work.** { *; }

# General safety for View Binding
-keep class com.robinzon.medicationwizard.databinding.** { *; }
