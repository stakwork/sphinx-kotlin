# Notifications

Sphinx Android (the `/sphinx/application/sphinx` module) compiles by default with the
`/sphinx/service/features/notifications/feature-service-notification-empty` module.

This provides other consuming modules with a stub implementations that does nothing.

## Enabling compilation with FirebaseMessaging (for push notifications):
 - Add the `google-services.json` file to directory:
 ```
 /sphinx/service/features/notifications/feature-service-notification-firebase/
 ```
 - In your global `gradle.properties` file (ex: `/home/$USER/.gradle/gradle.properties`) add the following:
 ```
 # Options: empty, firebase
 # Defaults to empty
 SPHINX_COMPILE_PUSH_NOTIFICATION_TYPE=firebase
 ```
