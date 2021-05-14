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

## Git Pull
In order to keep the submodules updated when pulling the latest code, run:
```
git pull --recurse-submodules
```

To update only the submodules, run:
```
git submodule update --remote
```

## Notifications
See [this](./sphinx/service/features/notifications/README.md) to enable building Sphinx
with FirebaseMessaging
