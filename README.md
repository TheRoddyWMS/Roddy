# Roddy?

Roddy is a framework for development and management of script based workflows on a batch processing cluster.

# Documentation

You can find our documentation under http://roddy-documentation.readthedocs.io

# Installation from release packages

Release ZIPs are available via [Github Releases](https://github.com/eilslabs/Roddy/releases). You first need a to set up some kind of runtime
environment into which you can install multiple versions of Roddy. Download the latest release of the RoddyEnv ZIP and unpack it.

After that you can install arbitrary releases of the Roddy ZIP into the dist/bin/ directory.   

# Build instructions

Roddy and its dependencies [BatchEuphoria](https://github.com/eilslabs/BatchEuphoria) and [RoddyToolLib](https://github.com/eilslabs/RoddyToolLib) use
the Gradle build system. Specifically, it uses the [composite build feature](https://docs.gradle.org/current/userguide/composite_builds.html) of 
Gradle. You can either let the gradle wrapper do all the work and download all dependecies including BatchEuphoria and RoddyToolLib for you, or you
get your own clones of the BatchEuphoria and RoddyToolLib git repos and reference them with the `--includeBuild` parameter like this

```
git clone https://github.com/eilslabs/RoddyToolLib.git
git clone https://github.com/eilslabs/BatchEuphoria.git
git clone https://github.com/eilslabs/Roddy.git
cd Roddy
./gradlew roddyDistZip --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/
./gradlew roddyEnvironmentDistZip --include-build ../RoddyToolLib/ --include-build ../BatchEuphoria/
```

The two example tasks build the distribution ZIPs for the Roddy core installed in `dist/bin/` and the runtime environment with the top-level starter
script `roddy.sh`. 