package com.toggl.buildsrc

object Versions {
    const val ktlint = "0.29.0"
}

object Libs {
    const val androidGradlePlugin = "com.android.tools.build:gradle:4.2.0-alpha05"
    const val gradleVersionsPlugin = "com.github.ben-manes:gradle-versions-plugin:0.28.0"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.2"
    // fixes warning thrown by SLF4J
    const val slf4j = "org.slf4j:slf4j-simple:1.7.26"

    object Android {
        const val desugarJdkLibs = "com.android.tools:desugar_jdk_libs:1.0.9"
    }

    object Test {
        const val kotestVersion = "4.1.3"

        const val junit4 = "junit:junit:4.13"
        const val robolectric = "org.robolectric:robolectric:4.4-alpha-2"
        const val junit5Plugin = "de.mannodermaus.gradle.plugins:android-junit5:1.6.2.0"
        const val kotlinTestRunner = "io.kotest:kotest-runner-junit5-jvm:$kotestVersion"
        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:1.4.0-rc"
        const val kotlinCoroutineTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.8-1.4.0-rc"
        const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:1.4.0-rc"
        const val mockk = "io.mockk:mockk:1.10.0"
        const val assertJ = "org.assertj:assertj-core:3.16.1"

        object Jupiter {
            private const val version = "5.6.1"
            // (Required) Writing and executing Unit Tests on the JUnit5 Platform
            const val api = "org.junit.jupiter:junit-jupiter-api:$version"
            const val engine = "org.junit.jupiter:junit-jupiter-engine:$version"
            // (Optional) If you need "Parameterized Tests"
            const val params = "org.junit.jupiter:junit-jupiter-params:$version"
        }
    }

    object Google {
        const val material = "com.google.android.material:material:1.1.0"
        const val googleServicesPluginClassPath = "com.google.gms:google-services:4.3.3"
        const val firebaseCrashlyticsPluginClassPath = "com.google.firebase:firebase-crashlytics-gradle:2.0.0"
        const val firebaseCore = "com.google.firebase:firebase-core:17.2.3"
        const val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics:17.0.0"
        const val firebasePerformance = "com.google.firebase:firebase-perf:19.0.7"
        const val firebasePerformancePluginClassPath = "com.google.firebase:perf-plugin:1.3.1"
        const val firebaseAnalytics = "com.google.firebase:firebase-analytics:17.4.0"
        const val gson = "com.google.code.gson:gson:2.8.6"
    }

    object Kotlin {
        private const val version = "1.4-M3"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val reflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"
    }

    object Coroutines {
        private const val version = "1.3.8-1.4.0-rc"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
    }

    object AndroidX {
        const val appcompat = "androidx.appcompat:appcompat:1.1.0"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.2.0-alpha03"
        const val browser = "androidx.browser:browser:1.0.0"
        const val fragment = "androidx.fragment:fragment:1.0.0"
        const val activityKtx = "androidx.activity:activity-ktx:1.2.0-alpha06"

        object Compose {
            const val version = "0.1.0-dev15"
            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val compiler = "1.4.0-dev-withExperimentalGoogleExtensions-20200720"
            object UI {
                const val core = "androidx.compose.ui:ui:$version"
                const val layout = "androidx.compose.foundation:foundation-layout:$version"
                const val material = "androidx.compose.material:material:$version"
                const val icons = "androidx.compose.material:material-icons-extended:$version"
                const val foundation = "androidx.compose.foundation:foundation:$version"
                const val animation = "androidx.compose.animation:animation:$version"
                const val tooling = "androidx.ui:ui-tooling:$version"
            }
        }

        object Hilt {
            private const val version = "1.0.0-alpha02"
            const val viewModel = "androidx.hilt:hilt-lifecycle-viewmodel:$version"
            const val compiler = "androidx.hilt:hilt-compiler:$version"
        }

        object Preference {
            private const val version = "1.1.0"
            const val core = "androidx.preference:preference:$version"
            const val ktx = "androidx.preference:preference-ktx:$version"
        }

        object Test {
            const val core = "androidx.test:core:1.3.0-rc01"
            const val runner = "androidx.test:runner:1.3.0-rc01"
            const val rules = "androidx.test:rules:1.3.0-rc01"
            const val espressoCore = "androidx.test.espresso:espresso-core:3.3.0-rc01"
        }

        const val constraintlayout = "androidx.constraintlayout:constraintlayout:1.1.3"

        const val coreKtx = "androidx.core:core-ktx:1.2.0"

        object Lifecycle {
            private const val version = "2.2.0"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val livedata = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val commonJava8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
        }

        object Room {
            private const val version = "2.2.5"
            const val common = "androidx.room:room-common:$version"
            const val runtime = "androidx.room:room-runtime:$version"
            const val compiler = "androidx.room:room-compiler:$version"
            const val ktx = "androidx.room:room-ktx:$version"
            const val testing = "androidx.room:room-testing:$version"
        }

        object Navigation {
            private const val version = "2.3.0"
            const val fragment = "androidx.navigation:navigation-fragment-ktx:$version"
            const val ui = "androidx.navigation:navigation-ui-ktx:$version"
        }
    }
    object Hilt {
        private const val version = "2.28-alpha"

        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val hilt = "com.google.dagger:hilt-android:$version"
        const val compiler = "com.google.dagger:hilt-android-compiler:$version"
    }

    object Microsoft {
        const val appCenterAnalytics = "com.microsoft.appcenter:appcenter-analytics:3.1.0"
    }

    object Arrow {
        private const val version = "0.10.4"
        const val optics = "io.arrow-kt:arrow-optics:$version"
        const val syntax = "io.arrow-kt:arrow-syntax:$version"
        const val meta = "io.arrow-kt:arrow-meta:$version"
    }

    object Square {
        const val retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
        const val gsonConverter = "com.squareup.retrofit2:converter-gson:2.9.0"
    }
}
