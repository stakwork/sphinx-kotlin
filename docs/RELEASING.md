# Releasing

 - Ensure notification settings are as desired (with or without FirebaseMessaging)
     - See [HERE](../sphinx/service/features/notifications/README.md) for more info
       about Notifications
 - Perform a clean release build
 ```bash
 $ ./gradlew clean
 $ ./gradlew build
 ```
 - Take out, and re-insert YubiKey for apk signing
 - Ensure `scripts/.pkcs11_java.cfg` has the correct directory location under `library` field
 - Run signing script
 ```bash
 $ scripts/sign_app_release_build.sh
 ```
 - Generated, signed apks for each CPU architecture will be located in
 ```
 sphinx/application/sphinx/build/outputs/apk/release
 ```