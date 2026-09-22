package com.todoapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.todoapp.data.local.converter.Converters
import com.todoapp.data.local.dao.CategoryDao
import com.todoapp.data.local.dao.DeletedListDao
import com.todoapp.data.local.dao.DeletedTaskDao
import com.todoapp.data.local.dao.TaskDao
import com.todoapp.data.local.dao.TodoListDao
import com.todoapp.data.local.dao.SyncOperationDao
import com.todoapp.data.local.entity.CategoryEntity
import com.todoapp.data.local.entity.DeletedListEntity
import com.todoapp.data.local.entity.DeletedTaskEntity
import com.todoapp.data.local.entity.TaskEntity
import com.todoapp.data.local.entity.TodoListEntity
import com.todoapp.data.local.entity.SyncOperationEntity

@Database(
    entities = [TaskEntity::class, CategoryEntity::class, DeletedTaskEntity::class, DeletedListEntity::class, TodoListEntity::class, SyncOperationEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deletedTaskDao(): DeletedTaskDao
    abstract fun deletedListDao(): DeletedListDao
    abstract fun todoListDao(): TodoListDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `lists` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `ownerId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `listId` TEXT")
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `deletedAt` INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_operations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `attemptCount` INTEGER NOT NULL DEFAULT 0,
                        `lastError` TEXT
                    )
                """)
                val publicListId = com.todoapp.domain.model.TodoList.PUBLIC_LIST_ID
                val personalListId = "list_personal"
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO `lists` (`id`, `name`, `type`, `ownerId`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(publicListId, "Public", "public", null, now, now)
                )
                db.execSQL(
                    "INSERT INTO `lists` (`id`, `name`, `type`, `ownerId`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf(personalListId, "Personal", "private", null, now, now)
                )
                db.execSQL("UPDATE `tasks` SET `listId` = ?", arrayOf(personalListId))
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_operations_entity
                    ON sync_operations(entityType, entityId)
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deleted_lists` (
                        `id` TEXT NOT NULL,
                        `deletedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` DROP COLUMN `deletedAt`")
            }
        }

        fun getInstance(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) return existing

                val db = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = db
                db
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val categories = listOf(
                    arrayOf("cat_work", "Work", 0xFF6750A4L, 0),
                    arrayOf("cat_personal", "Personal", 0xFF006D3CL, 1),
                    arrayOf("cat_shopping", "Shopping", 0xFF9C4300L, 2),
                    arrayOf("cat_study", "Study", 0xFF0061A4L, 3),
                    arrayOf("cat_health", "Health", 0xFFBA1A1AL, 4)
                )
                for (cat in categories) {
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (id, name, color, position) VALUES (?, ?, ?, ?)",
                        cat
                    )
                }
            }
        }
    }
}
