# Day

A personal daily planner and time-blocking app for Android with a neumorphic UI, home screen widget, habit tracking, and an integrated assistant.

---

## What is Day?

Day is built around a simple idea: your daily schedule, to-do list, habit tracker, and quick notes shouldn't live in four separate apps.

Instead of juggling multiple tools, Day gives you an interactive home screen widget for quick check-ins, a clean time-blocked daily view, and an on-device assistant that can actually manage your schedule for you.

---

## Features

### 📅 Daily Time-Blocking (Today Tab)
- Hourly timeline with color-coded task blocks
- Day navigation (`<` / `>`) to review past days or plan ahead
- Daily score and completion streak tracking
- Daily quote (rotating or custom)

### ✅ Tasks & Recurring To-Dos (Todo Tab)
- Priority markers (P1, P2, P3) and due dates
- Recurring rules: daily, weekly, or custom days (advances automatically upon completion)
- To-dos with assigned times automatically link into Today's schedule

### 🌱 Habit Tracking & Analytics (Habits Tab)
- Habits at rest stay time-free until scheduled into Today
- Tap-to-expand habit cards:
  - **Weekly check**: Monday–Sunday status indicators (`✓` / `·` / `✗`)
  - **30-Day Heatmap**: Canvas contribution grid showing completions and target hours reached
  - **Stats**: Current streak, all-time best streak, monthly consistency rate, and average session duration

### 📝 Notes
- Clean scratchpad for thoughts, meeting notes, or ideas
- Color-tagged cards with tap-to-edit

### 🤖 Day — The Assistant
- An integrated assistant that can read and modify your schedule
- **Tool integration**: Day can add tasks, reschedule conflicts, complete todos, track habits, and save timetable notes directly
- **Proactive suggestions**: Surfaces actionable nudges (habits at risk after 6 PM, neglected routines, overdue tasks, or morning reminders to plan open days)
- **Voice input**: Hold or tap to dictate in Indian English (`en-IN`) with real-time speech transcription
- **Smart NLP input bar**: Type natural sentences like *"Math lecture from 2 to 4pm"* or *"Pay electricity bill tomorrow at 6pm"* to auto-schedule
- **Cached & token-optimized**: In-memory query caching and trimmed snapshots to keep latency and API usage minimal

### 📱 Home Screen Widget
- Interactive Jetpack Glance widget
- Toggle between Today, Todos, and Notes without opening the app
- Mark tasks or todos complete directly from the home screen

### 🎨 Theming
- Neumorphic soft-shadow interface
- Independent color selection for the app and the widget (including deep dark mode `#363636` and custom hex accents)

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose & Material 3
- **Widgets**: Jetpack Glance (AppWidget)
- **Database**: Room Database (v5 with migrations)
- **Concurrency**: Coroutines & StateFlow
- **Voice**: Android SpeechRecognizer
- **AI**: Google Generative AI (Gemini Flash API)
- **Build**: Gradle Kotlin DSL, Min SDK 24, Target SDK 36

---

## Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17
- Android device or emulator running API 24+

### Build & Run
```bash
git clone https://github.com/arnavchawla15/Day.git
cd Day
./gradlew assembleDebug
```
Or open the project in Android Studio and hit **Run**.

### Gemini API Key (Optional)
The core app (tasks, todos, habits, notes, widgets, NLP regex fallback) works fully offline.

To enable the Day assistant and AI scheduling:
1. Get a free API key from [Google AI Studio](https://aistudio.google.com/).
2. Open Day → tap the palette / settings icon in the top bar.
3. Paste your key in the **Gemini API Key** field.

---

## License

All Rights Reserved.
