# R8 rules, applied to any application minifying this module, Android's release builds for example.
#
# DomainType.domainType(Class) names the domain type for the class's simple name, read at runtime, and a remote
# connection sends that name to the server, which finds the domain by it: with the class renamed, a minified Fieldbook
# failed its sync with "Domain: xb1 has not been defined". This keeps the names of the classes declaring a static
# DomainType field, and the field, which covers the usual DomainType DOMAIN = domainType(Chinook.class) in a domain's
# API. A domain type named for another class needs a rule of the application's own, see domainType(Class). DomainTest
# checks this names DomainType.
-keepclasseswithmembers,allowshrinking,allowoptimization class * {
	static is.codion.framework.domain.DomainType *;
}
