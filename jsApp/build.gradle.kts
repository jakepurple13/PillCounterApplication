plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("kotlinx-serialization")
}

kotlin {
    js(IR) {
        browser {
            useCommonJs()
            binaries.executable()
        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                //implementation(project(":common"))
                implementation("com.juul.kable:core:0.21.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.web.core)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.material3)
            }
        }
    }
}

compose.experimental {
    web.application {}
}

/*
afterEvaluate {
    rootProject.extensions.configure<NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
        versions.webpackCli.version = "4.10.0"
    }
}
*/
