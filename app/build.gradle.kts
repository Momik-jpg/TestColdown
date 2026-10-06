import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.tasks.Copy
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val localBuildDir = System.getenv("LOCALAPPDATA")
    ?.let { file("$it/ExamCountdownBuild/app") }
    ?: file("${rootDir}/.build/app")
layout.buildDirectory.set(localBuildDir)

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystoreProperties = keystorePropertiesFile.exists()
if (hasKeystoreProperties) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.andrin.examcountdown"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.andrin.examcountdown"
        minSdk = 26
        targetSdk = 34
        versionCode = 30
        versionName = "1.6.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasKeystoreProperties) {
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                if (!storeFilePath.isNullOrBlank()) {
                    storeFile = rootProject.file(storeFilePath)
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasKeystoreProperties) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-beta.7"
            // The separate test app does not require the production signing key.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Ship the exact runtime dependency inventory and original upstream notices offline.
// Resolve POM metadata using Gradle's repositories/cache rather than HTTP in the app.
listOf("debug", "release", "preview").forEach { variant ->
    val output = layout.buildDirectory.dir("generated/legalAssets/$variant")
    android.sourceSets.getByName(variant).assets.srcDir(output)
    val generate = tasks.register("generate${variant.replaceFirstChar { it.uppercaseChar() }}LegalNotices") {
        val runtime = configurations.getByName("${variant}RuntimeClasspath")
        inputs.files(runtime)
        inputs.files(rootProject.file("LICENSE"), rootProject.file("PRIVACY.md"),
            rootProject.file("docs/accessibility.md"), rootProject.file("docs/graphics-provenance.md"),
            rootProject.file("third-party/Apache-2.0.txt"))
        outputs.dir(output)
        doLast {
            val destination = output.get().dir("legal").asFile.apply { mkdirs() }
            rootProject.file("PRIVACY.md").copyTo(destination.resolve("privacy.md"), overwrite = true)
            rootProject.file("docs/accessibility.md").copyTo(destination.resolve("accessibility.md"), overwrite = true)
            val factory = DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            fun Element.child(name: String): Element? = (0 until childNodes.length)
                .map { childNodes.item(it) }.filterIsInstance<Element>().firstOrNull { it.tagName == name }
            val pomCache = mutableMapOf<String, Element>()
            fun pom(group: String, name: String, version: String): Element {
                val key = "$group:$name:$version"
                return pomCache.getOrPut(key) {
                    val result = dependencies.createArtifactResolutionQuery().forModule(group, name, version)
                        .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute()
                    val artifact = result.resolvedComponents.flatMap { it.getArtifacts(MavenPomArtifact::class.java) }
                        .filterIsInstance<ResolvedArtifactResult>().singleOrNull()
                        ?: throw GradleException("Missing license metadata: $key")
                    factory.newDocumentBuilder().parse(artifact.file).documentElement
                }
            }
            fun licenses(group: String, name: String, version: String, visited: Set<String> = emptySet()): List<String> {
                val key = "$group:$name:$version"
                if (key in visited) throw GradleException("Cyclic license metadata: $key")
                val project = pom(group, name, version)
                val section = project.child("licenses")
                if (section != null) {
                    return (0 until section.childNodes.length).map { section.childNodes.item(it) }
                        .filterIsInstance<Element>().filter { it.tagName == "license" }.map { license ->
                            listOfNotNull(license.child("name")?.textContent?.trim(), license.child("url")?.textContent?.trim())
                                .filter { it.isNotBlank() }.joinToString(" · ")
                        }.filter { it.isNotBlank() }
                }
                val parent = project.child("parent") ?: return emptyList()
                return licenses(parent.child("groupId")!!.textContent.trim(), parent.child("artifactId")!!.textContent.trim(),
                    parent.child("version")!!.textContent.trim(), visited + key)
            }
            val components = runtime.incoming.resolutionResult.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
                .distinctBy { it.displayName }.sortedBy { it.displayName }
            val inventory = components.joinToString("\n\n") { module ->
                val declared = licenses(module.group, module.module, module.version)
                if (declared.isEmpty() || declared.any { it.contains("\${") }) {
                    throw GradleException("Review missing or unresolved license: ${module.displayName}")
                }
                "## ${module.displayName}\n\n${declared.joinToString("\n") }"
            }
            val notices = mutableListOf<String>()
            val noticeName = Regex("(?i)(^|/)(license[^/]*|notice[^/]*|copying[^/]*|AL2\\.0|LGPL2\\.1)(\\.[^/]*)?$")
            runtime.resolvedConfiguration.resolvedArtifacts.sortedBy { it.moduleVersion.id.toString() }.forEach { artifact ->
                if (artifact.file.extension in listOf("aar", "jar")) ZipFile(artifact.file).use { zip ->
                    zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                        if (noticeName.containsMatchIn(entry.name)) {
                            val body = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                            notices += "## ${artifact.moduleVersion.id} · ${entry.name}\n\n$body"
                        } else if (entry.name == "classes.jar" || entry.name.startsWith("libs/") && entry.name.endsWith(".jar")) {
                            ZipInputStream(zip.getInputStream(entry)).use { nested ->
                                var member = nested.nextEntry
                                while (member != null) {
                                    if (!member.isDirectory && noticeName.containsMatchIn(member.name)) {
                                        val body = nested.readBytes().toString(Charsets.UTF_8)
                                        notices += "## ${artifact.moduleVersion.id} · ${entry.name}/${member.name}\n\n$body"
                                    }
                                    member = nested.nextEntry
                                }
                            }
                        }
                    }
                }
            }
            destination.resolve("licenses.md").writeText(listOf(
                "# Lizenzen & Bildnachweise\n\nOffline-Nachweise für die Laufzeitbibliotheken dieser $variant-Ausgabe.",
                "## TestColdown · MIT\n\n" + rootProject.file("LICENSE").readText(),
                rootProject.file("docs/graphics-provenance.md").readText(),
                "# Bibliotheken mit aufgelösten Versionen\n\n$inventory",
                "# Apache License 2.0\n\n" + rootProject.file("third-party/Apache-2.0.txt").readText(),
                "# Originale Hinweise aus den Bibliotheken\n\n" + notices.distinct().joinToString("\n\n")
            ).joinToString("\n\n"))
            logger.lifecycle("Bundled ${components.size} runtime license entries and ${notices.size} original notices for $variant")
        }
    }
    tasks.matching { it.name == "pre${variant.replaceFirstChar { it.uppercaseChar() }}Build" }.configureEach { dependsOn(generate) }
}

tasks.register("bundlePlayRelease") {
    group = "release"
    description = "Build signed Play Store AAB and copy it to dist/."
    if (hasKeystoreProperties) {
        dependsOn("bundleRelease", "copyReleaseAabToDist")
    } else {
        doFirst {
            throw GradleException(
                "Missing keystore.properties. Copy keystore.properties.example and fill your upload key values."
            )
        }
    }
}

tasks.register<Copy>("copyReleaseAabToDist") {
    group = "release"
    description = "Copy release AAB to dist as ExamCountdown-release.aab."
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    into(rootProject.layout.projectDirectory.dir("dist"))
    rename { "ExamCountdown-release.aab" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.13.0")

    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
