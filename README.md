# NPHIIS Surveillance (Surveillance)

NPHIIS Surveillance is an Android application for public health surveillance and response workflows. The app supports integrated case-based surveillance, structured data capture via FHIR questionnaires, and synchronized reporting to a remote FHIR server. It is built with Kotlin, AndroidX, and the Google Android FHIR SDK.

## Table of Contents
- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Data Capture Assets](#data-capture-assets)
- [Configuration](#configuration)
- [Getting Started](#getting-started)
- [Build & Run](#build--run)
- [Testing](#testing)
- [Utilities](#utilities)
- [Troubleshooting](#troubleshooting)
- [Security & Privacy](#security--privacy)
- [License](#license)

## Project Overview
This repository contains a single-module Android app (`:app`) named **NPHIIS**. The app targets Android API 35+, with minimum support for API 26. It integrates with a FHIR backend for data exchange and includes workflows for surveillance, case listing, and questionnaire-driven data entry.

## Key Features
- **FHIR-based data capture** using structured questionnaires stored in assets.
- **Case listing and case details** flows for surveillance reporting.
- **Periodic sync** using WorkManager and background sync workers.
- **Push notifications** via Firebase Messaging.
- **Location capture** for case reporting (fine/coarse location permissions).
- **App update support** via Play Core update APIs.

## Tech Stack
- **Kotlin** with AndroidX (Activity, Fragment, Navigation, Lifecycle).
- **Google Android FHIR SDK** (`engine`, `data-capture`).
- **Retrofit + OkHttp** for network calls.
- **Firebase** (Crashlytics, Cloud Messaging).
- **WorkManager** for background sync.
- **Material Components** + Lottie for UI.

## Project Structure
```
.
├── app
│   ├── src/main
│   │   ├── AndroidManifest.xml
│   │   ├── assets/                # FHIR questionnaire JSON files
│   │   ├── java/com/icl/nphi       # App source
│   │   └── res/                    # UI resources
│   ├── build.gradle.kts
│   └── google-services.json
├── build.gradle.kts
├── gradle/libs.versions.toml
└── settings.gradle.kts
```

## Data Capture Assets
Questionnaire definitions are stored under `app/src/main/assets` and include forms for different surveillance workflows (e.g., MOH 505, measles, mpox, contact tracing, lab results). These assets are loaded by the app at runtime for structured data entry.

## Configuration
### Backend Base URL
The FHIR server endpoint is defined in `Constants.kt`:
```
const val BASE_URL = "https://dsrfhir.intellisoftkenya.com/hapi/fhir/"
```
Update this value to point at your target FHIR server environment.

### Firebase
`app/google-services.json` is included for Firebase services (Crashlytics, Messaging). If you plan to use your own Firebase project, replace this file with your project’s configuration.

### Android SDK
- **compileSdk:** 36
- **targetSdk:** 35
- **minSdk:** 26

## Getting Started
### Prerequisites
- Android Studio (latest stable recommended).
- JDK 11 (aligned with Gradle settings).
- Android SDK with API 35+ installed.

### Clone
```
git clone https://github.com/IntelliSOFT-Consulting/Public-Health-Surveillance-and-Response.git
cd Public-Health-Surveillance-and-Response
```

## Build & Run
### Debug build (local)
```
./gradlew :app:assembleDebug
```

### Run on device/emulator
Open the project in Android Studio and run the **app** configuration.

### Release build
```
./gradlew :app:assembleRelease
```

## Testing
### Unit tests
```
./gradlew :app:testDebugUnitTest
```

### Instrumentation tests
```
./gradlew :app:connectedDebugAndroidTest
```

## Utilities
### Package renaming script
A helper script exists to rename the package and optionally build the APK:
```
cd app
./packager.sh <new.package.name>
```

## Troubleshooting
- **Gradle sync issues:** Ensure you are using JDK 11 and have the required Android SDK platforms installed.
- **Firebase errors:** Verify `google-services.json` matches your Firebase project and package name.
- **Network errors:** Confirm the `BASE_URL` is reachable and supports FHIR endpoints.

## Security & Privacy
This application handles sensitive health data. Ensure:
- Transport uses HTTPS endpoints.
- Access tokens and credentials are managed securely.
- Device storage is encrypted and protected by OS-level security.
- You comply with local data protection requirements.

## License
[![License](http://img.shields.io/:license-gnu-blue.svg?style=flat-square)](http://badges.gnu-license.org) 

Licensed under the GNU General Public License, Version 3.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://github.com/IntelliSOFT-Consulting/Public-Health-Surveillance-and-Response/blob/main/LICENSE.md

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
