# Add project specific ProGuard rules here.
# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep public class * extends android.webkit.WebView
-dontwarn android.webkit.**
