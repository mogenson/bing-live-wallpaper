plugins {
    id("com.android.application")
    id("com.goodanser.clj-android.android-clojure")
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
            isMinifyEnabled = true
            isShrinkResources = true
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
        getByName("test") {
            resources.srcDirs("src/test/clojure")
        }
    }

    packaging {
        resources {
            // runtime-repl's Android-compatible stubs shadow nREPL's originals
            pickFirsts += listOf("nrepl/socket.clj", "nrepl/socket/dynamic.clj")
        }
    }
}

clojureOptions {
    warnOnReflection.set(true)
}

dependencies {
    implementation("org.clojure:clojure:1.12.0")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
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


