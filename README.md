# TodoApp

A modern offline-first Android todo application built with Kotlin and Jetpack Compose, featuring Supabase sync and Material 3 design.

![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/Android-SDK%2035-3DDC84?logo=android)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

<!-- Screenshots: add actual screenshots to a screenshots/ directory and uncomment
| ![Today](screenshots/today.png) | ![Tasks](screenshots/tasks.png) | ![Calendar](screenshots/calendar.png) | ![Settings](screenshots/settings.png) |
-->

## Features

**Task Management**
- Create, edit, complete, and delete tasks
- Pin important tasks to the top
- Three priority levels: High, Medium, Low
- Due dates and times with visual indicators
- Recurring tasks (daily, weekly, monthly, yearly) with configurable interval and end date
- Reminders via system notifications

**Organization**
- Public List (shared across all users) and private lists
- Categories with color coding (Work, Personal, Shopping, Study, Health by default)
- Search by title and description
- Multi-level filtering: task type (All, Today, Upcoming, Overdue, Done, Pinned), priority, and category

**Screens**
- **Today** — Overdue and today's tasks with progress indicator and undo-delete
- **Tasks** — All tasks with search, list selector, and filter chips
- **Calendar** — Monthly grid with task dots and selected-date task list
- **Add/Edit** — Progressive disclosure form with schedule, organization, recurrence, and reminder settings
- **Settings** — Theme, notifications, categories, sync, lists, and backup/restore

**Sync & Offline**
- Fully offline-first — Room is the local source of truth
- Supabase sync with conflict resolution (newer mutation wins)
- Tombstone-based deletion with 30-day retention

**Other**
- Light, dark, and system theme with Android 12+ dynamic color
- Home-screen widget (Glance) showing today's tasks
- Backup and restore via JSON export/import
- Edge-to-edge layout with bottom navigation

## Architecture

```
UI (Compose)
  ↓
ViewModel (StateFlow<UiState>)
  ↓
Use Case
  ↓
Repository (interface)
  ↓
Room (local)  ←→  SyncEngine  ←→  Supabase (remote)
```

- **UI Layer** — Jetpack Compose screens, reusable components (`TaskItem`, `EmptyState`, `SectionHeader`, `SettingsRow`, `SettingsSection`)
- **ViewModel Layer** — `AndroidViewModel` subclasses exposing `StateFlow<UiState>`, undo-delete pattern
- **Domain Layer** — Use cases for tasks, categories, lists, preferences, backup
- **Data Layer** — Room database, Supabase client, repository implementations
- **Sync Engine** — Push/pull with conflict resolution, operation collapsing, tombstone lifecycle
- **DI** — Manual dependency injection via `AppModule` singleton (no Hilt/Dagger)

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.1.0 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Navigation | Navigation Compose | 2.8.5 |
| Local Database | Room | 2.6.1 |
| Remote Sync | Supabase Kotlin SDK | 2.6.1 |
| HTTP Client | Ktor OkHttp | 2.3.12 |
| Widget | Glance | 1.1.1 |
| Serialization | Kotlinx Serialization JSON | 1.7.3 |
| Preferences | DataStore | 1.1.1 |
| Build System | Gradle | 8.11.1 |
| Android Gradle Plugin | AGP | 8.7.3 |
| Min SDK | 26 | — |
| Target/Compile SDK | 35 | — |
| JVM Target | 17 | — |

## Project Structure

```
TodoApp/
├── .github/workflows/        # CI and CodeQL workflows
├── app/src/main/java/com/todoapp/
│   ├── data/
│   │   ├── local/            # Room DB, DAOs, entities, converters
│   │   ├── remote/           # Supabase client, auth, config, models
│   │   ├── repository/       # Repository implementations
│   │   └── sync/             # SyncEngine (725 lines)
│   ├── di/                   # AppModule (manual DI singleton)
│   ├── domain/
│   │   ├── model/            # Task, TodoList, Category, TaskFilter, ThemeMode
│   │   ├── repository/       # Repository interfaces
│   │   ├── usecase/          # backup, category, list, preferences, task
│   │   └── util/             # DateUtils, IdGenerator
│   ├── notification/         # AlarmReceiver, BootReceiver, NotificationHelper
│   └── ui/
│       ├── components/       # TaskItem, SharedComponents
│       ├── navigation/       # NavGraph, Screen
│       ├── screens/          # today, tasks, calendar, settings, addedit
│       ├── theme/            # Color, Dimens, Shape, Theme, Type
│       └── widget/           # TodoWidget, TodoWidgetReceiver, WidgetHelper
├── app/src/test/             # 11 test files
├── supabase/migrations/      # Database schema and RLS
└── gradle/                   # Wrapper
```

## Getting Started

### Requirements

- **JDK 17**
- **Android SDK** with API 35
- **Android Studio** (latest stable recommended)

### Clone

```bash
git clone https://github.com/CyferBoy/Todo_List_App.git
cd Todo_List_App
```

### Configure

Create a `.env.local` file in the project root:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

The root `build.gradle.kts` reads `.env.local` and passes these values as `BuildConfig` fields to the app.

> **Never** commit `.env.local` — it is already in `.gitignore`. Never use a Supabase service-role key in the Android app.

### Build

```bash
./gradlew assembleDebug
```

The debug APK is output to `app/build/outputs/apk/debug/`.

## Supabase Setup

The app requires a Supabase project for cloud sync. Without it, the app still works fully offline.

### 1. Create a Supabase project

Go to [supabase.com](https://supabase.com) and create a project.

### 2. Run the migration

Execute `supabase/migrations/001_lists_and_rls.sql` in the Supabase SQL editor. This:

- Creates `tasks` and `lists` tables
- Inserts the default Public List (`00000000-0000-0000-0000-000000000001`)
- Enables Row Level Security with policies for public/private data
- Enables Realtime for both tables
- Creates indexes for query performance

### 3. Enable anonymous authentication

In the Supabase dashboard, go to **Authentication → Providers** and enable **Anonymous Sign-In**.

### 4. Configure the app

Add your project URL and anon key to `.env.local` (see [Configure](#configure) above).

### Row Level Security

| Table | Public List | Private Lists |
|-------|------------|---------------|
| `lists` | Readable by everyone, cannot be modified by users | Full access for owner |
| `tasks` | Full access for all authenticated users | Only owner can access |

The app uses **anonymous authentication** — each install gets a unique anonymous user ID. No login UI is needed.

## Configuration

| File | Purpose | Committed? |
|------|---------|-----------|
| `.env.local` | Supabase URL and anon key | No (gitignored) |
| `gradle.properties` | JVM args, AndroidX, Kotlin style | Yes |
| `app/build.gradle.kts` | App config, dependencies, ProGuard | Yes |

The `.env.local` loader is in the root `build.gradle.kts`. It reads key=value pairs and sets them as Gradle properties, which are then passed to `BuildConfig`.

## Build and Run

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

Or open the project in Android Studio and run on an emulator or device (min SDK 26).

## Offline-First and Synchronization

The app is designed to work fully without internet:

1. **Room is the source of truth** — all reads and writes go to the local database first
2. **Sync operations are queued** — changes create entries in the `sync_operations` table
3. **SyncEngine processes the queue** — pushes local changes to Supabase, then pulls remote changes
4. **Conflict resolution** — newer mutation timestamp wins; equal timestamps are treated as already converged
5. **Tombstones** — deleted entities are tracked for 30 days to ensure sync propagation, then cleaned up
6. **Operation collapsing** — redundant operations are merged (e.g., INSERT + UPDATE → INSERT)

The app remains fully functional during sync failures. Local data is never destroyed by a failed sync.

## Backup and Restore

Available in **Settings → Data**.

**Export** creates a JSON file containing:
- All tasks
- All categories
- All lists

**Import** replaces all local data with the backup contents:
- Validates backup structure, required fields, and referential integrity
- Reassigns private list ownership to the current user
- Normalizes the Public List to the fixed shared ID
- Clears the sync queue after import
- Runs in a database transaction for atomicity

> **Warning**: Import replaces all existing data. Export first if you want to keep current data.

## Widget

A Glance-powered home-screen widget showing:

- "My Tasks" header with the Public List name
- Today's date
- Up to 3 tasks with checkboxes (tap to toggle completion)
- "+N more" indicator when additional tasks exist

Refreshes automatically when tasks are created, edited, completed, or deleted.

## Testing

11 unit test files covering:

| Test File | Focus |
|-----------|-------|
| `TaskTest` | Task model fields, defaults, priorities, recurrence |
| `CategoryTest` | Category creation and equality |
| `ListTest` | Public/Private list types and properties |
| `DateUtilsTest` | Date formatting, epoch round-trip, greetings |
| `SyncTest` | Conflict resolution, tombstones, multi-device convergence |
| `SyncQueueTest` | Operation queuing, collapsing, attempt tracking |
| `BackupTest` | Backup validation, owner reassignment, atomicity |
| `SecurityTest` | Public list immutability, RLS correctness |
| `RecurrenceTest` | Daily/weekly/monthly/yearly recurrence, leap years |
| `RecurrenceConfigTest` | Recurrence field persistence |
| `ReminderTest` | Reminder scheduling, cancellation, recurrence chaining |

Run all tests:

```bash
./gradlew testDebugUnitTest
```

## Code Quality

- **Clean Architecture** — domain/data/ui separation with repository and use-case patterns
- **MVVM** — ViewModels expose immutable `StateFlow<UiState>`
- **Manual DI** — `AppModule` singleton, no framework overhead
- **Offline-first** — Room as source of truth, sync as background process
- **CI** — GitHub Actions runs unit tests, lint, and debug build on every push/PR
- **CodeQL** — Security analysis via GitHub CodeQL (java-kotlin + actions)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-change`)
3. Make focused changes
4. Run tests: `./gradlew testDebugUnitTest`
5. Keep database migrations backward-compatible
6. Open a pull request

**Rules:**
- Never commit secrets (`.env.local`, API keys, service-role keys)
- Never put Supabase service-role keys in the Android app
- Never bypass Row Level Security
- Don't break offline behavior — Room must remain the source of truth
- Don't bypass the repository/use-case architecture

## Release

Current version: **1.0.0** (versionCode 1)

See [GitHub Releases](https://github.com/CyferBoy/Todo_List_App/releases) for release notes.

## License

```
Copyright 2025 CyferBoy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Security

See [SECURITY.md](SECURITY.md) for the security policy.

If you discover a security vulnerability, please report it responsibly. Do not open a public issue for security vulnerabilities.
