# Add project specific ProGuard rules here.
# Keep Room entities
-keep class com.financeobserver.model.** { *; }

# Keep parser implementations
-keep class com.financeobserver.parser.** { *; }
