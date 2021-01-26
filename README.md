[![CircleCI](https://circleci.com/gh/freeletics/RxSmartLock.svg?style=svg)](https://circleci.com/gh/freeletics/RxSmartLock)

[ ![Download](https://maven-badges.herokuapp.com/maven-central/com.freeletics.rxsmartlock/rxsmartlock/badge.svg) ](https://maven-badges.herokuapp.com/maven-central/com.freeletics.rxsmartlock/rxsmartlock)

# RxSmartLock

This library is a wrapper for the [SmartLock](https://www.howtogeek.com/354482/what-is-google-smart-lock-exactly/) for passwords. It makes all interactions with SmartLock API through the reactive streams.

# How to use

To save credentials use the function `storeCredentials()`:
```kotlin
RxGoogleSmartLockManager.storeCredentials(context, credentials).subscribe()
```

To retrieve stored credentials use the function `retrieveCredentials()`:
```kotlin
RxGoogleSmartLockManager.retrieveCredentials(context).subscribe()
```

There are a few other methods to interract with SmartLock defined in the [interface](https://github.com/freeletics/RxSmartLock/blob/master/rxsmartlock/src/main/java/com/freeletics/rxsmartlock/SmartLockManager.kt).

### Testing

For testing purposes there is [EmptySmartLockManager](https://github.com/freeletics/RxSmartLock/blob/master/rxsmartlock/src/main/java/com/freeletics/rxsmartlock/EmptySmartLockManager.kt). The methods of that class are empty. Inject `EmptySmartLockManager` for testing purposes to the classes you are testing.

# Dependency
Dependencies are hosted on Maven Central:

```gradle
implementation 'com.freeletics.rxsmartlock:rxsmartlock:1.1.1'
```
Keep in mind that this library is written in kotlin which means you also need to add `kotlin-stdlib` to a project using RxSmartLock.

### Snapshot
Latest snapshot (directly published from master branch):

```gradle
allprojects {
    repositories {
        // Your repositories.
        // ...
        // Add url to snapshot repository
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
}

```

```gradle
implementation 'com.freeletics.rxsmartlock:rxsmartlock:1.1.2-SNAPSHOT'
```


# License

```
Copyright 2021 Freeletics

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
