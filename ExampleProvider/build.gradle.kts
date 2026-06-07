dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use an integer for version numbers
version = 1

cloudstream {
    description = "Nonton film Indonesia di LK21"
    authors = listOf("rajwaslthn")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = true
    language = "id"
    iconUrl = "https://www.google.com/s2/favicons?domain=lk21online.mom&sz=256"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    }
}