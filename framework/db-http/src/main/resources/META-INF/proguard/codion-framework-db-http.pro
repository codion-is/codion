# R8 rules, applied to any application minifying this module, Android's release builds for example.
#
# JdkHttpTransport uses java.net.http, absent on Android, where HttpTransport finds java.net.http.HttpClient missing by
# name and uses UrlConnectionTransport instead, JdkHttpTransport never loaded. R8 refuses to build with the classes
# referenced and absent unless told not to warn. HttpTransportTest checks this names java.net.http.
-dontwarn java.net.http.**
