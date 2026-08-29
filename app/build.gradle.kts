plugins {
    id("com.android.application")
}

android {
    namespace = "com.binglivewallpaper"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.binglivewallpaper"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            resources.srcDirs("src/main/clojure")
        }
        getByName("test") {
            resources.srcDirs("src/test/clojure")
        }
    }
}

dependencies {
    implementation("org.clojure:clojure:1.12.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}

android.applicationVariants.all {
    val variant = this
    val variantCapName = variant.name.replaceFirstChar { it.uppercase() }
    val clojureOutputDir = layout.buildDirectory.dir("intermediates/clojure/${variant.name}/classes").get().asFile

    val compileClojureTask = tasks.register<JavaExec>("compile${variantCapName}Clojure") {
        dependsOn(variant.javaCompileProvider)
        group = "build"
        description = "Compiles Clojure sources for ${variant.name}."

        mainClass.set("clojure.main")

        val cljSourceDir = file("src/main/clojure")
        val compileClasspath = files(
            cljSourceDir,
            clojureOutputDir,
            android.bootClasspath,
            variant.javaCompileProvider.map { it.classpath }
        )

        classpath = compileClasspath
        systemProperty("clojure.compile.path", clojureOutputDir.absolutePath)
        systemProperty("clojure.compiler.direct-linking", "true")

        doFirst {
            clojureOutputDir.mkdirs()
        }

        args(
            "-e",
            """
            (doseq [ns ['com.binglivewallpaper.image-fetcher
                        'com.binglivewallpaper.image-store
                        'com.binglivewallpaper.refresh-worker
                        'com.binglivewallpaper.wallpaper-engine
                        'com.binglivewallpaper.wallpaper-service
                        'com.binglivewallpaper.boot-receiver]]
              (println "AOT compiling" ns "...")
              (compile ns))
            """.trimIndent()
        )
    }

    variant.registerPostJavacGeneratedBytecode(files(clojureOutputDir).builtBy(compileClojureTask))
}

val testClojure = tasks.register<JavaExec>("testClojure") {
    group = "verification"
    description = "Runs Clojure unit tests."

    mainClass.set("clojure.main")

    val cljSourceDir = file("src/main/clojure")
    val cljTestDir = file("src/test/clojure")
    val testTaskProvider = tasks.named<Test>("testDebugUnitTest")

    classpath = files(
        cljSourceDir,
        cljTestDir,
        android.bootClasspath,
        testTaskProvider.map { it.classpath }
    )

    args(
        "-e",
        """
        (require '[clojure.test :as t]
                 '[com.binglivewallpaper.refresh-worker-test])
        (let [results (t/run-tests 'com.binglivewallpaper.refresh-worker-test)]
          (when (pos? (+ (:fail results) (:error results)))
            (System/exit 1)))
        """.trimIndent()
    )
    dependsOn("testDebugUnitTest")
}

tasks.matching { it.name == "test" || it.name == "check" }.configureEach {
    dependsOn(testClojure)
}


