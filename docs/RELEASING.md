# Releasing

 - Checkout the newly created tag
 ```bash
 $ git fetch
 $ git checkout <tag>
 $ git pull --recurse-submodule
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
