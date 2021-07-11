# Releasing

## Preparing Repository For A Release
 - Checkout branch `develop`
 ```bash
 $ git checkout develop
 $ git pull --recurse-submodule
 ```
 - Update the following variables in the root project's `gradle.properties` file:
   - `VERSION_NAME`
   - `VERSION_CODE`
   - `KA_VERSION_NAME`
   - `KA_VERSION_CODE`
 - Update `docs/CHANGELOG.md` with features/fixes
 - Build the app to ensure no errors exist
 ```bash
 $ ./gradlew clean
 $ ./gradlew :sphinx:application:sphinx:build
 ```
 - Commit changes via (replace `<version name>` with actual name, ex: `1.0.0-alpha01`)
     - `$ git add --all`
     - `$ git commit --message "Prepare release <version name>"`
         - Optionally, if you sign commits with GPG keys, add the `-S` flag
 - Create a tag for the new version (replace `<version name>` with actual name)
     - `$ git tag <version name> -m "Release v<version name>"`
         - Optionally, if you sign commits with GPG keys, add the `-s` flag
 - Push
 ```bash
 $ git push origin <version name>
 $ git push develop
 ```
 - Merge to branch `master`
     - `$ git checkout master`
     - `$ git pull`
     - `$ git merge --no-ff develop`
         - Optionally, if you sign commits with GPG keys, add the `-S` flag
 - Push
 ```bash
 $ git push
 ```

## Building A Release
 - Checkout the newly created tag
 ```bash
 $ git fetch
 $ git checkout <tag>
 $ git submodule update
 ```
 - Ensure notification settings are as desired (with or without FirebaseMessaging)
     - See [HERE](./NOTIFICATIONS.md) for more info about Notifications
 - Perform a clean release build
 ```bash
 $ ./gradlew clean
 $ ./gradlew :sphinx:application:sphinx:build
 ```
 - Ensure that OpenSC is installed on your machine
     - Linux:
         - `sudo apt-get update && sudo apt-get install opensc-pkcs11 -y`
     - Windows:
         - See [Here](https://github.com/OpenSC/OpenSC/wiki/Windows-Quick-Start)
     - MacOS:
         - See [Here](https://github.com/OpenSC/OpenSC/wiki/macOS-Quick-Start)
 - Take out, and re-insert YubiKey for apk signing
 - Ensure `scripts/.pkcs11_java.cfg` has the correct directory location under `library` field
     - Linux Example:
         - `library = /usr/lib/x86_64-linux-gnu/opensc-pkcs11.so`
 - Ensure that the `ANDROID_SDK` environment variable is set
     - Linux Example from Terminal:
         - `export ANDROID_SDK=/home/administrator/Android/Sdk`
 - Run signing script
 ```bash
 $ scripts/sign_app_release_build.sh
 ```
 - Signed apks for each CPU architecture will be located in `sphinx/application/sphinx/build/outputs/apk/release`
     - Signed apks will contain `signed` in their names.
 - Create a release on GitHub for the given tag used when building the app.
 - Add to the release description a link to the ChangeLog by copy/pasting the following:
     - `See [ChangeLog](./docs/CHANGELOG.md)`
