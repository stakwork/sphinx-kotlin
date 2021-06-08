# Releasing

 - Ensure notification settings are as desired (with or without FirebaseMessaging)
     - See [HERE](../sphinx/service/features/notifications/README.md) for more info
       about Notifications
 - Perform a clean release build
 ```bash
 $ ./gradlew clean
 $ ./gradlew build
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
 - Generated, signed apks for each CPU architecture will be located in
 ```
 sphinx/application/sphinx/build/outputs/apk/release
 ```