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

dependencies {
    // LibGDX Core
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    
    // Desktop backend
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    
    // Box2D for physics (essential for smooth platformer movement)
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
    
    // Tools for better performance
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
    
    // Utilities
    implementation("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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