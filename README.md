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
- Tap-to-edit task cards with Neumorphic time pickers and deletion
- Share schedule: one-tap export to clipboard and Android system share sheet (WhatsApp, Telegram, Messages, etc.)
- Scheduled to-dos appear with their allotted start time (e.g., *"At 2:00 PM"*) rather than a dummy end-time
- Daily score and completion streak tracking
- Daily quote (rotating or custom)

### ✅ Tasks & Recurring To-Dos (Todo Tab)
- **Custom To-Do Lists**: Create and toggle between custom lists (Work, Personal, Errands, etc.) with dedicated task counts
- **Subtasks & Checklist Steps**: Add, manage, and tick off incremental steps within any to-do
- **Reliable Recurring Rules**: Daily, weekly, or custom day recurrences automatically persist and roll forward to Today's agenda every single day, even if missed on a previous day
- Priority markers (P1, P2, P3, P4) and due dates with clock dial time pickers
- Bidirectional synchronization: ticking off a to-do in the Todo tab immediately marks any corresponding scheduled task complete in Today and on the widget

### 🔔 Notifications & Reminders
- System notifications for scheduled task start times and to-do deadlines
- `AlarmManager` integration with `BOOT_COMPLETED` receiver to persist reminders across device reboots
- Toggleable in Settings with full Android 13+ runtime permission support

### 🌱 Habit Tracking & Analytics (Habits Tab)
- Habits at rest stay time-free until scheduled into Today
- Tap-to-expand habit cards:
  - **Weekly check**: Monday–Sunday status indicators (`✓` / `·` / `✗`)
  - **30-Day Heatmap**: Canvas contribution grid showing completions and target hours reached
  - **Stats**: Current streak, all-time best streak, monthly consistency rate, and average session duration

### 📝 Notes & Folders (Notes Tab)
- Scratchpad for thoughts, meeting memos, and ideas
- **Folder Organization**: Group notes into custom folders (Ideas, Work, Journal, etc.) with horizontal pill navigation
- Quick inline editing and timestamp tracking

### 🤖 Day — The Assistant
- An integrated assistant that can read and modify your schedule
- **Tool integration**: Day can add tasks, reschedule conflicts, complete todos, track habits, and save timetable notes directly
- **Proactive suggestions**: Surfaces actionable nudges (habits at risk after 6 PM, neglected routines, overdue tasks, or morning reminders to plan open days)
- **Voice input**: Hold or tap to dictate in Indian English (`en-IN`) with real-time speech transcription
- **Smart NLP input bar**: Type natural sentences like *"Math lecture from 2 to 4pm"* or *"Pay electricity bill tomorrow at 6pm"* to auto-schedule
- **Cached & token-optimized**: In-memory query caching and trimmed snapshots to keep latency and API usage minimal

### 📱 Home Screen Widget
- Interactive Jetpack Glance widget with instant, zero-lag button toggling
- Toggle between Today, Todos, and Notes directly from your home screen
- Bidirectional task and to-do ticking with debounced action callbacks
- Clean deduplicated Today view showing tasks and unscheduled due items with WCAG AAA contrast

### 🎨 Theming
- Neumorphic soft-shadow interface with extruded and inset physical depths
- Independent color selection for the app and widget, featuring adapted dark mode `#363636` contrast optimization

---

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose & Material 3
- **Widgets**: Jetpack Glance (AppWidget)
- **Database**: Room Database (v6 with safe SQLite migrations)
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
