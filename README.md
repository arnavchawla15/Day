<div align="center">
  <h1>🌅 Day - The Ultimate Productivity Hub</h1>
  <p>
    <strong>A seamless, widget-first Android application that combines Tasks, Todos, Notes, Habits, and a Pomodoro Timer—all powered by AI.</strong>
  </p>
</div>

---

## 🚀 About Day

**Day** is not just another to-do list. It's a comprehensive, all-in-one productivity suite built natively for Android using Modern Android Development (MAD) practices. 

With a strong emphasis on a **Widget-First** experience, **Day** is designed to keep you productive without ever having to open the app. The home screen widgets serve as interactive command centers, and the integrated Gemini AI turns your natural language inputs into fully organized schedules.

## ✨ Key Features

- **Widget-First Experience**: Fully interactive Glance app widgets for your Daily Agenda, Habit Tracking, and a live Pomodoro Timer. Mark tasks complete and start focus sessions directly from your home screen.
- **Smart AI Parsing (NLP)**: Powered by Google's Gemini AI. Simply type "Schedule a meeting from 2pm to 3pm and remind me to call mom", and Day will automatically extract the tasks, times, and todos.
- **Integrated Habit Tracking**: Build routines with dedicated Habit Widgets that allow one-tap completions.
- **Pomodoro Timer**: A native Pomodoro timer accessible via a Quick Settings tile or Home Screen Widget to keep your deep work sessions undisturbed.
- **Fully Custom Theming Engine**: Personalize your workspace! Independently customize the exact HEX colors and dark/light modes for the App UI and your Widgets.
- **Daily Inspiration**: Start your day right with a customizable daily quote system on your main dashboard.

## 🛠 Tech Stack

Built with cutting-edge Android technologies:

- **Language**: Kotlin 
- **UI Toolkit**: Jetpack Compose & Material 3
- **Widgets**: Jetpack Glance (Compose for App Widgets)
- **Architecture**: MVVM with Kotlin Coroutines & Flows
- **Local Persistence**: Room Database & DataStore Preferences
- **AI Integration**: Google Generative AI SDK (Gemini API)
- **Build System**: Gradle with Kotlin DSL

## 📦 Getting Started

### Prerequisites
- Android Studio (Latest version)
- JDK 17
- An Android Device or Emulator (API 26+)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/arnavchawla15/Day.git
   ```
2. **Open the project:**
   Open the `Day` directory in Android Studio.
3. **Sync and Build:**
   Allow Gradle to sync, then hit the **Run** button to install the app on your device or emulator.

### Setting up the Gemini AI
To unlock the natural language smart-input feature, you will need a free Gemini API key:
1. Head over to [Google AI Studio](https://aistudio.google.com/) and generate a free API key.
2. Open the **Day** app, navigate to **Settings**.
3. Paste your API key into the Gemini API Key section.

## 🎨 Customizing Themes

Day features a highly advanced theming engine that separates your App theme from your Widget theme.
- Navigate to **Settings > App Theme** or **Settings > Widget Theme**.
- Choose from 20 beautifully curated pastel aesthetics.
- Or, select **Custom** and enter your own specific Hex code (e.g., `#FF5722`) to instantly generate a full Material 3 color palette matching your choice!

## 📱 Project Structure

- `day/app/src/main/java/com/example/day` - Core logic, ViewModels, and App UI.
- `day/app/src/main/java/com/example/day/widget` - Jetpack Glance Widget implementations (DayWidget, HabitWidget, PomodoroWidget).
- `day/app/src/main/java/com/example/day/data` - Room Database DAOs and entities.
- `day/app/src/main/java/com/example/day/theme` - Dynamic Custom Theme generator logic.

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

Distributed under the MIT License. See `LICENSE` for more information.
