package dev.aliziwa.dispatch.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DispatchDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2() {
        // Create database version 1 with a test record
        helper.createDatabase(testDb, 1).apply {
            execSQL(
                """
                INSERT INTO work_orders (id, title, site, priority, status, version, updatedAt)
                VALUES ('wo-1', 'Test Order', 'Bay 1', 'HIGH', 'OPEN', 1, 1000)
                """.trimIndent()
            )
            close()
        }

        // Re-open database and validate AutoMigration to version 2
        val db = helper.runMigrationsAndValidate(testDb, 2, true)

        // Query the migrated row and verify default isPending value is 0 (false)
        val cursor = db.query("SELECT id, title, isPending FROM work_orders WHERE id = 'wo-1'")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals("wo-1", cursor.getString(0))
        assertEquals("Test Order", cursor.getString(1))
        assertEquals(0, cursor.getInt(2)) // Default isPending = 0
        cursor.close()
    }
}
