group = "com.expectale"
version = "3.0.0"

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.nova)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    implementation(libs.nova)
    // the protection integration is declared against the public API module
    compileOnly("xyz.xenondevs.nova:nova-api:${libs.versions.nova.get()}")
}

addon {
    // The lowercase name is the addon id, so this has to stay "deep_storage":
    // recipes, configs, lang keys and every unit already placed in a world use that namespace.
    name = "Deep_Storage"
    version = project.version.toString()
    main = "com.expectale.DeepStorage"
    authors = listOf("CptBeffHeart", "awakcon1234")
    description = "Bulk item storage in cells, with security cards"
    website = "https://github.com/awakcon1234/DeepStorage"

    // output directory for the generated addon jar is read from the "outDir" project property (-PoutDir="...")
    val outDir = project.findProperty("outDir")
    if (outDir is String)
        destination.set(File(outDir))
}
