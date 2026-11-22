# R8 rules, applied to any application minifying this module, Android's release builds for example.
#
# H2 is kept whole. Its MVStore writes the class names of its data types into the database file,
# org.h2.mvstore.db.RowDataType$Factory for example, and loads them by name when the file is opened, so with its
# classes renamed a database written by one release, or by a debug build, would be unreadable by the next. It loads its
# file systems by name as well, out of R8's sight, Android's DriverManager finds the driver inside the platform, where
# R8 doesn't see it either, and this module reaches org.h2.tools.RunScript and org.h2.store.fs.FileUtils by name, H2
# being the application's dependency, not this module's. Harmless without H2. H2DatabaseTest checks these name H2.
-keep class org.h2.** { *; }

# The classes H2 refers to from its optional features, absent on Android: the servlet console, AWT, JMX, JNDI,
# scripting, compiling Java functions, XA, Lucene full text search, JTS geometries and OSGi.
-dontwarn jakarta.servlet.**
-dontwarn java.awt.**
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn javax.naming.**
-dontwarn javax.script.**
-dontwarn javax.servlet.**
-dontwarn javax.tools.**
-dontwarn javax.transaction.xa.**
-dontwarn jdk.net.**
-dontwarn org.apache.lucene.**
-dontwarn org.locationtech.jts.**
-dontwarn org.osgi.**
-dontwarn java.io.ObjectInputFilter
-dontwarn java.io.ObjectInputFilter$Config
-dontwarn java.sql.JDBCType
-dontwarn java.sql.SQLType
-dontwarn javax.security.auth.callback.NameCallback
-dontwarn javax.security.auth.login.LoginContext
-dontwarn javax.sql.XAConnection
-dontwarn javax.sql.XADataSource
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLOutputFactory
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn javax.xml.stream.XMLStreamWriter
-dontwarn javax.xml.transform.stax.StAXResult
-dontwarn javax.xml.transform.stax.StAXSource
