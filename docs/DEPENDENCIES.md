# DEPENDENCIES

## Adding Dependencies
 - Open the `gradle/dependencies.gradle` file
     - Dependencies are categorized by `deps`, `debugDeps`, `plugin`, `testDeps`, 
     & `kaptDeps`, and ordered alphabetically.
 - Add your dependency to the appropriate section.

## Updating Dependencies
 - Check for updates by running:
 `$ ./gradlew dependencyUpdates`
 - Update the desired dependency versions in the `gradle/dependencies.gradle` file
