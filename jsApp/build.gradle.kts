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
                implementation(projects.common)
                implementation(compose.web.core)
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
