# wukong-robosats-android

An Android client for https://github.com/RoboSats/robosats made by [BitcoinWukong](https://getalby.com/p/bitcoinwukong)

## Prerequisites

Before you begin, ensure you have met the following requirements:

- Android Studio
- JDK (Java Development Kit)
- An Android device or emulator for testing


## Building and Running the App

### Setting Up the Environment

1. Clone the repository:
   ```bash
   git clone https://github.com/BitcoinWukong/wukong-robosats-android.git
   ```
1. Open the project in Android Studio.
1. Wait for Android Studio to install all the necessary Gradle dependencies.

### Building the App
1. In Android Studio, go to Build -> Make Project to build the app.
1. Alternatively, you can use the command line to install the app directly to your Android device:
```bash
./gradlew installDebug
```

You may need to specify android sdk location via env var `ANDROID_HOME` if you never built the project in Android Studio before. On MacOS and Linux, the default location android sdk is `$HOME/Library/Android/sdk`:
```bash
ANDROID_HOME=$HOME/Library/Android/sdk ./gradlew installDebug
```

### License
[GNU Affero General Public License v3.0](https://github.com/BitcoinWukong/wukong-robosats-android/blob/main/LICENSE)
