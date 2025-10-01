# MyTask

MyTask is an Android app built with Jetpack Compose to help users manage tasks and events with seamless Google Calendar integration and AI-driven notifications. The app allows users to create tasks, events, and routines, sync them to the device calendar, and receive smart reminders based on task completion patterns.

## Features
- **Task Management**: Create tasks with title, time, category (e.g., Personal, Design), and date, synced to Google Calendar with a 15-minute reminder.
- **Event Management**: Add events with start/end times, location, and optional weekly/monthly recurrence, plus up to two reminders.
- **Routines**: 
  - **Manual**: Create recurring routines (daily, weekly, monthly) via a dedicated dialog.
  - **AI-Driven**: Automatically detects routines based on task completion patterns (5+ completions within a 30-minute window) and syncs them as daily recurring calendar events.
- **AI Notifications**: Powered by `NotifAi`, sends context-aware reminders with nudge phrases, escalating based on missed tasks.
- **Calendar Sync**: Writes tasks, events, and routines to the device’s default calendar using `CalendarContract`.
- **Responsive UI**: Built with Jetpack Compose, featuring a tabbed interface (Reminders, Events) and FABs for adding tasks, events, and routines.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material3, Accompanist Pager)
- **Calendar**: Android `CalendarContract` for read/write
- **Background Tasks**: WorkManager for periodic evaluations
- **Persistence**: Gson for storing task patterns and AI profile
- **Minimum SDK**: 26 (Android 8.0 Oreo)

## Setup
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/algo1127/MyTask.git
   cd MyTask
