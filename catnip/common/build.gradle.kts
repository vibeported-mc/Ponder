plugins {
    alias(libs.plugins.loom)
    alias(libs.plugins.configure.platform)
}

dependencies {
    minecraft(libs.minecraft)
    compileOnly(libs.bundles.mixin)
    // ModConfigSpec: the common module only sees vanilla, so the config definitions need the port.
    compileOnly(libs.forgeconfigapiport.common)
}

loom {
    accessWidenerPath = file("catnip_common_source.accesswidener")
}
