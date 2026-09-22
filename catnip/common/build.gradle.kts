plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.configure.platform)
}

dependencies {
    minecraft(libs.minecraft)
    compileOnly(libs.bundles.mixin)
    // ModConfigSpec: the common module only sees vanilla, so the config definitions need the port.
    compileOnly(libs.forgeconfigapiport.common)
    // ModConfig/ModConfigs: ConfigHelper looks a mod's configs up through FML, always present at runtime.
    compileOnly(libs.fml.loader) { isTransitive = false }
}

loom {
    accessWidenerPath = file("catnip_common_source.accesswidener")
}
