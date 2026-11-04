# ProGuard rules for WakeMeThere

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep entities and DAOs
-keep class com.wakemethere.app.data.local.** { *; }
-keep class com.wakemethere.app.data.model.** { *; }

# Google Play Services Location & Maps
-keep class com.google.android.gms.location.** { *; }
-keep class com.google.android.gms.maps.** { *; }

# Compose rules are automatically handled by Compose compiler & BOM

