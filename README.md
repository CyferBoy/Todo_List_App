# TodoApp

A modern Android task manager built with Jetpack Compose and Material 3, featuring offline-first sync via Supabase.

## Features

- **Material 3 Design** — Green/teal palette with light/dark theme support
- **Offline-First Sync** — Supabase backend with automatic conflict resolution
- **Task Management** — Create, edit, complete, pin, and delete tasks
- **Smart Scheduling** — Due dates, times, reminders, and recurring tasks
- **Organization** — Lists, categories, and priority levels (High/Medium/Low)
- **Search & Filter** — Full-text search with task type, priority, and category filters
- **Calendar View** — Monthly calendar with task indicators and date selection
- **Home Widget** — Glance-powered widget for quick task overview
- **Backup & Restore** — JSON export/import for data portability
- **Multiple Lists** — Public (shared) and private task lists

## Architecture

```
UI (Compose) → ViewModel → UseCase → Repository → Room ↔ SyncEngine ↔ Supabase
```

- **Room** — Local SQLite database (v5) with SQLCipher encryption
- **Supabase** — Remote sync with PostgreSQL and Row Level Security
- **Hilt-free DI** — Manual dependency injection via `AppModule` singleton
- **Clean Architecture** — Domain layer with use cases, data layer with repositories

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM, Use Cases, Repository Pattern |
| Local DB | Room 2.6.1 (SQLCipher encrypted) |
| Remote | Supabase Kotlin SDK 2.6.1, Ktor OkHttp |
| Widget | Glance 1.1.1 |
| Build | Kotlin 2.1.0, KSP, Compose BOM 2024.12.01 |

## Screens

- **Today** — Overdue and today's tasks with progress indicator
- **Tasks** — All tasks with search, list selector, and multi-level filters
- **Calendar** — Monthly view with task dots and selected-date task list
- **Add/Edit** — Progressive disclosure form with schedule, organization, and recurrence
- **Settings** — Theme, notifications, categories, sync, lists, and backup

## Project Structure

```
app/src/main/java/com/todoapp/
├── data/
│   ├── local/         # Room database, DAOs, entities
│   ├── remote/        # Supabase client, auth, models
│   ├── repository/    # Repository implementations
│   └── sync/          # SyncEngine with conflict resolution
├── di/                # AppModule (manual DI)
├── domain/
│   ├── model/         # Task, TodoList, Category, etc.
│   ├── repository/    # Repository interfaces
│   ├── usecase/       # Business logic
│   └── util/          # DateUtils, IdGenerator
├── notification/      # Reminder scheduling
├── ui/
│   ├── components/    # TaskItem, SharedComponents
│   ├── navigation/    # NavGraph, Screen routes
│   ├── screens/       # Today, Tasks, Calendar, Settings, AddEdit
│   ├── theme/         # Color, Typography, Shape, Dimens
│   └── widget/        # Glance widget
└── MainActivity.kt
```

## Getting Started

1. Clone the repository
2. Create `local.properties` with:
   ```properties
   SUPABASE_URL=your_supabase_url
   SUPABASE_ANON_KEY=your_supabase_anon_key
   ```
3. Run `./gradlew assembleDebug`

## Sync Setup

The app uses Supabase for remote sync. Run the migration in `supabase/migrations/001_lists_and_rls.sql` to set up the database schema with Row Level Security.

## CI/CD

GitHub Actions workflow (`.github/workflows/android-ci.yml`):
- Unit tests on every push/PR
- Lint checks
- Debug APK build
- CodeQL Advanced security analysis (weekly + on PR)

## License

MIT
