# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/google/home/mangini/tools/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

-verbose

# remove code paths that has the SDK int less than 21 up to 1000
-allowaccessmodification
-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 23..1000;
}

# Keep kotlin metadata annotations
-keep class kotlin.Metadata

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep, allowobfuscation, allowshrinking interface retrofit2.Call
-keep, allowobfuscation, allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep, allowobfuscation, allowshrinking class kotlin.coroutines.Continuation

-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.

# Kodein Specifics
-keep, allowobfuscation, allowoptimization class org.kodein.type.TypeReference
-keep, allowobfuscation, allowoptimization class org.kodein.type.JVMAbstractTypeToken$Companion$WrappingTest

-keep, allowobfuscation, allowoptimization class * extends org.kodein.type.TypeReference
-keep, allowobfuscation, allowoptimization class * extends org.kodein.type.JVMAbstractTypeToken$Companion$WrappingTest
