# MyTask ⏰

> **A personal Android productivity app with calendar integration and AI-driven notifications**  
> *Currently in active development - not yet production-ready*

---

## 🚨 Important Notice

This is a **personal learning project**. While I'm passionate about building this, please understand:

- ❗ **Not guaranteed to work perfectly** - There are known bugs and unstable features
- 🔧 **Still under development** - Many features are incomplete or experimental
- ⚠️ **Use at your own risk** - Not suitable for mission-critical tasks yet
- 📱 **Personal use only for now** - No official APK releases until features stabilize

I will consider releasing APK files once most core features work reliably and bugs are minimized. Until then, you're welcome to clone and experiment with the codebase as a learning resource.

---

## ✨ Project Philosophy

I built MyTask because I wanted a task manager that actually feels alive and learns from my habits. Instead of static reminders, I wanted an AI companion that understands my patterns and pushes me in ways that work. This was my journey into understanding Android architecture, Jetpack Compose, and how to build something personalized from scratch.

If this sparks ideas for your own projects—or if you find inspiration in my approach—please feel free to copy, modify, and learn from this code however you see fit.

---

## 🎯 What It Does

| Feature | Status | Description |
|---------|--------|-------------|
| **Task Management** | ✅ Working | Create tasks with title, time, category, and date |
| **Event Management** | ⚠️ Partial | Add events with times, locations, basic recurrence |
| **Calendar Sync (Write)** | ✅ Working | Tasks/events saved to device calendar |
| **Calendar Sync (Read)** | ⚠️ In Progress | Reading from calendar has stability issues |
| **AI Notifications** | ❌ Not Working | Smart reminders with escalating tones based on patterns |
| **Routine Detection** | 🔄 Work-in-Progress | Auto-detects patterns after 5+ similar completions |
| **Persistent Storage** | ✅ Working | Task patterns saved with Gson/SharedPreferences |
| **UI** | ✅ Polished | Modern Jetpack Compose interface with animations |

---

## 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM-inspired, Single Activity |
| **Calendar API** | Android `CalendarContract` Provider |
| **Background Processing** | WorkManager |
| **Persistence** | Gson + SharedPreferences (no Room) |
| **Min SDK** | 26 (Android 8.0 Oreo) |
| **Target SDK** | 34 (Android 14) |

---

## 📦 Quick Start

### Clone the Repository

```bash
git clone https://github.com/algo1127/MyTask.git
cd MyTask
