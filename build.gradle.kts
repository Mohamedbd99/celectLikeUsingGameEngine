plugins {
    id("java")
    id("application")
}

group = "org.celestelike"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val gdxVersion = "1.12.1"
configurations.configureEach {
    exclude(group = "org.lwjgl.lwjgl")
    exclude(group = "net.java.jinput")
}
val tools by configurations.creating

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")

    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")

    // tools only (ne doit pas partir dans distZip)
    tools("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
}

tasks.register<JavaExec>("extractTiles") {
    group = "tools"
    description = "Extract tiles from a TSX tileset into tile_0000.png, tile_0001.png, ..."
    mainClass.set("org.celestelike.tools.TsxTileExtractor")
    classpath = sourceSets["main"].runtimeClasspath + tools
    args("C:/Users/moham/Downloads/kenney_pico-8-platformer/Transparent/Tiles/manara.tsx")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.celestelike.desktop.DesktopLauncher")
}

tasks.register<JavaExec>("runViewEditor") {
    group = "application"
    description = "Launches the standalone ViewEditor"
    mainClass.set("org.celestelike.desktop.EditorLauncher")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runInspector") {
    group = "application"
    description = "Launches the map inspector tool"
    mainClass.set("org.celestelike.desktop.InspectorLauncher")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// Fix for Windows: Use wildcard classpath to avoid "command line too long" error
// Java supports wildcards in classpath (since Java 6): lib/* includes all JARs
tasks.withType<CreateStartScripts> {
    doLast {
        // For Windows batch file, use wildcard classpath and change to APP_HOME
        val windowsScript = file("$outputDir/$applicationName.bat")
        if (windowsScript.exists()) {
            var windowsContent = windowsScript.readText()
            // Replace the long CLASSPATH line with a wildcard (Java accepts forward slashes on Windows too)
            windowsContent = windowsContent.replace(
                Regex("set CLASSPATH=.*"),
                "set CLASSPATH=%APP_HOME%/lib/*"
            )
            // Change to APP_HOME directory before running (ensures consistent working directory)
            windowsContent = windowsContent.replace(
                Regex(":execute\\s+@rem Setup the command line"),
                ":execute\n@rem Change to APP_HOME to ensure consistent working directory\ncd /d \"%APP_HOME%\"\n@rem Setup the command line"
            )
            windowsScript.writeText(windowsContent)
        }
        
        // For Unix script, also use wildcard for consistency and simplicity
        val unixScript = file("$outputDir/$applicationName")
        if (unixScript.exists()) {
            val unixContent = unixScript.readText()
            // Match CLASSPATH= followed by $APP_HOME/lib/ and all JARs, replace with wildcard
            val pattern = "CLASSPATH=" + "\\" + "$" + "APP_HOME/lib/[^\\n]*"
            val replacement = "CLASSPATH=" + "\\" + "$" + "APP_HOME/lib/*"
            unixScript.writeText(
                unixContent.replace(Regex(pattern), replacement)
            )
        }
    }
}


distributions {
    main {
        contents {
            // ✅ virer LWJGL2 + jinput du dossier lib/ de la distribution
            exclude("lib/gdx-backend-lwjgl-*.jar")
            exclude("lib/lwjgl-2.*.jar")
            exclude("lib/lwjgl-platform-2.*.jar")
            exclude("lib/jinput-*.jar")
            exclude("lib/jutils-*.jar")
            
            // Copy JSON files to distribution root so they're found regardless of working directory
            from(".") {
                include("editor_blueprint.json")
                include("inspector_snapshot.json")
                include("enemy_spawns.json")
                into(".")
            }
        }
    }
}
