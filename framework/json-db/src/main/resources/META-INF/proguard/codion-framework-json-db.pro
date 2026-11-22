# R8 rules, applied to any application minifying this module, Android's release builds for example.
#
# Jackson builds ErrorEnvelope through its @JsonCreator constructor and reads it through its @JsonProperty accessors,
# found by reflection, which R8 would otherwise remove as unused, or strip of their annotations. These keep them in the
# module's own classes. ErrorEnvelopeTest checks they name what exists.
-keepclassmembers class is.codion.framework.json.db.** {
	@com.fasterxml.jackson.annotation.JsonCreator <init>(...);
	@com.fasterxml.jackson.annotation.JsonProperty <methods>;
}
