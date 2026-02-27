# Pulse – Utility App (CP5307)

This is an Android application developed for Assessment 1: Utility App in CP5307.
It extends the starter structure using Jetpack Compose and Material Design 3 to implement a fitness-focused utility application.
Pulse is designed as a lightweight fitness utility tool that provides quick workout access and simple health metric calculations in an at-a-glance format.

---

## Getting Started
### How to Run
1. Clone or download this repository
2. Open in Android Studio
3. Run on an emulator or physical device (API 26+ recommended)

---

## Composables

### AppRoot()
- Sets up NavHost navigation
- Manages global theme (Dark Mode) state
- Defines navigation routes (Login, Main, Training, Session)

### MainScreen()
- Contains the main Scaffold layout
- Displays bottom navigation (Home / Settings)
- Handles top tab navigation (Recommend, Courses, Plans, Routes)
- Controls switching between Home and Settings content

### RecommendPage()
- Displays search functionality
- Shows quick feature grid (Running, Moves, Yoga, Walking, etc.)
- Displays recommended workout cards
- Navigates to workout detail

### TrainingScreen()
- Displays workout information
- Shows workout mode (Countdown or Stopwatch)
- Navigates to session screen

### TrainingSessionScreen()
- Implements countdown timer logic
- Implements stopwatch mode
- Supports pause, resume, restart, and end session

### SettingsPage()
- Allows users to configure:
    - Training intensity (Easy / Normal / Hard)
    - Countdown sound toggle
    - Vibration feedback toggle
    - Dark mode toggle
- Accepts body metric input:
    - Height
    - Weight
    - Age
    - Sex
- Dynamically calculates:
    - BMI
    - Estimated Body Fat Percentage

---

## Key Concepts Covered

| Week | Concept                        | Used In                          |
|------|--------------------------------|----------------------------------|
| 1    | Kotlin + Android Studio         | MainActivity.kt |
| 2    | Jetpack Compose Layouts         | Scaffold, LazyColumn, Cards, Tabs   |
| 3    | Material Design 3               | Theme, Buttons, NavigationBar |
| 4    | Navigation (NavHost) |AppRoot(), route handling        |
| 5    | State Management  | remember, rememberSaveable          |
| 6    | Coroutines  | Countdown & Stopwatch logic          |

---

## Suggested Extensions
-  Refactor timer and calculation logic into a ViewModel
- Improve separation between UI and business logic
- Simplify navigation to better align with pure utility principles
- Add data persistence for user preferences

---

## 📚 License
This project is developed for educational purposes in CP5307.
It may be modified and extended for assessment submission.
