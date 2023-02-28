import java.net.InetAddress
import java.net.NetworkInterface

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    kotlin("native.cocoapods")
    id("kotlinx-serialization")
    id("com.codingfeline.buildkonfig")
}

group = "com.programmersbox"
version = "1.0-SNAPSHOT"

@OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
kotlin {
    android {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        browser()
    }
    ios()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "common"
            isStatic = true
        }
        extraSpecAttributes["resources"] = "['src/commonMain/resources/**', 'src/iosMain/resources/**']"
    }
    sourceSets {
        val ktorVersion = extra["ktor.version"] as String
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.ui)
                api(compose.foundation)
                api(compose.materialIconsExtended)
                api(compose.material3)
                api("io.ktor:ktor-client-core:$ktorVersion")
                api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                api("io.ktor:ktor-client-logging:$ktorVersion")
                api("io.ktor:ktor-client-websockets:$ktorVersion")
                api("com.splendo.kaluga:bluetooth:0.5.0")
                api("com.splendo.kaluga:permissions:0.5.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4!!")
                api("moe.tlaster:precompose:1.3.14")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.0")
                api("androidx.core:core-ktx:1.9.0")
                //api("io.ktor:ktor-client-android:$ktorVersion")
                api("io.ktor:ktor-client-cio:$ktorVersion")
                api(projects.database)
                api("org.jmdns:jmdns:3.5.8")
                api("com.github.LeandroSQ:android-ble-made-easy:1.8.0")
                api("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
                api("com.github.zqisheng.QsBle:ble:1.2.3")
                api("com.github.zqisheng.QsBle:ble_coroutines:1.2.3")
                api("com.juul.kable:core:0.21.0")
                api("com.google.accompanist:accompanist-permissions:0.29.1-alpha")
                api("com.splendo.kaluga:base-permissions:0.5.0")
            }
        }

        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }

        val desktopMain by getting {
            dependencies {
                api(compose.preview)
                api("io.ktor:ktor-client-cio:$ktorVersion")
                api(projects.database)
                api("org.jmdns:jmdns:3.5.8")
            }
        }

        val desktopTest by getting

        val jsMain by getting {
            dependencies {
                api(compose.web.core)
                api("io.ktor:ktor-client-js:$ktorVersion")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependencies {
                api("io.ktor:ktor-client-darwin:$ktorVersion")
                api(projects.database)
                api("com.juul.kable:core:0.21.0")
            }
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }

    explicitApi()
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

fun getIP(): String {
    var result: InetAddress? = null
    val interfaces = NetworkInterface.getNetworkInterfaces()
    while (interfaces.hasMoreElements()) {
        val addresses = interfaces.nextElement().inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (!address.isLoopbackAddress) {
                if (address.isSiteLocalAddress) {
                    return address.hostAddress
                } else if (result == null) {
                    result = address
                }
            }
        }
    }
    return (result ?: InetAddress.getLocalHost()).hostAddress
}

buildkonfig {
    packageName = "com.programmersbox.common"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "serverLocalIpAddress",
            getIP()
        )
    }
}