# sphinx-kotlin

## Cloning
This repository uses git submodules (for the time being). When cloning the repository, run:
```
git clone --recursive https://github.com/stakwork/sphinx-kotlin.git
```

If you've already cloned the repository, run:
```
git checkout master
git pull
git submodule update --init
```

In order to keep the submodules updated when pulling the latest code, run:
```
git pull --recurse-submodules
```

To update only the submodules, run:
```
git submodule update --remote
```

## Notifications
See [this](./docs/NOTIFICATIONS.md) to enable building Sphinx with FirebaseMessaging

## Building The App
The App module to build is located at `sphinx/application/sphinx`. To Build, you can either:
 - Use Android Studio
 - From CommandLine via: `./gradlew :sphinx:application:sphinx:build`
