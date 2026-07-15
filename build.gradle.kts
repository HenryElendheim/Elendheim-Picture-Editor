// Top level build file. Plugins are declared here with "apply false" so the
// versions are shared, then each module turns the ones it needs on.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
