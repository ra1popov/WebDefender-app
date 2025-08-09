# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/romanpopov/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class androidx.** { *; }
-keep class android.** { *; }

-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.persistence.Basic
-dontwarn javax.persistence.Column
-dontwarn javax.persistence.Entity
-dontwarn javax.persistence.EnumType
-dontwarn javax.persistence.Enumerated
-dontwarn javax.persistence.FetchType
-dontwarn javax.persistence.GeneratedValue
-dontwarn javax.persistence.Id
-dontwarn javax.persistence.JoinColumn
-dontwarn javax.persistence.ManyToOne
-dontwarn javax.persistence.OneToMany
-dontwarn javax.persistence.OneToOne
-dontwarn javax.persistence.Table
-dontwarn javax.persistence.Version

-keepattributes Signature
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations

# Do not obfuscate classes from libraries.
-keep class retrofit2.** { *; }
-keep class io.reactivex.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Do not obfuscate classes from the dev.doubledot.doki library and its dependencies.
-keep class dev.doubledot.doki.** { *; }
-keep class ru.noties.markwon.** { *; }

