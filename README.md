# Day

Day is an Android productivity app built with Kotlin, Jetpack Compose, Room, and Android widgets. It helps users plan their day with tasks, todos, notes, habits, and a quick-add experience designed for fast capture.

## Highlights

- Task planning with a daily calendar-style workflow
- Todo and note capture for quick logging
- Habit tracking with scheduled routines
- Home-screen widgets for fast interaction
- AI-assisted parsing through Gemini for natural-language input

## Tech stack

- Kotlin
- Jetpack Compose
- Room Database
- Navigation 3
- Glance widgets
- Gradle with Kotlin DSL

## Getting started

1. Install Android Studio and JDK 17.
2. Clone this repository.
3. Open the `day` folder in Android Studio.
4. Sync Gradle and run the app on an emulator or device.
5. If you want to use the Gemini-based natural-language features, add your API key in the app settings.

## Project structure

- `day/app` contains the Android application module
- `day/app/src/main/java` contains the app source code
- `day/app/src/main/res` contains resources and widget definitions

## Notes

- The app uses `local.properties` for local SDK configuration and should not be committed with secrets.
- Widget and quick-add features are part of the main experience, so they are worth testing on a real device.
