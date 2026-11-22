# R8 rules, applied to any application minifying this module, Android's release builds for example.
#
# Jackson reads a TypeReference's type argument from the generic signature of its anonymous subclass,
# EntityObjectMapper's ENTITY_LIST_REFERENCE for example, and those an application registers. In full mode R8 keeps
# a signature only on kept classes, so these keep TypeReference and its subclasses, which may still be renamed, or
# removed when unused, as Gson's rules do for its TypeToken. EntityObjectMapperTest checks they name TypeReference.
-keep,allowobfuscation,allowshrinking class com.fasterxml.jackson.core.type.TypeReference
-keep,allowobfuscation,allowshrinking class * extends com.fasterxml.jackson.core.type.TypeReference
