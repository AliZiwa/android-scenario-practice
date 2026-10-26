package dev.aliziwa.timesheet.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TimesheetDatabase_Impl : TimesheetDatabase() {
  private val _shiftDao: Lazy<ShiftDao> = lazy {
    ShiftDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "55686f2add14e5061c7163282df7e6a8", "6e3013a8eb49afbf51cf5612a4102f97") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `shifts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workOrderId` TEXT NOT NULL, `date` TEXT NOT NULL, `startMinutes` INTEGER NOT NULL, `endMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `note` TEXT, `billable` INTEGER NOT NULL, `status` TEXT NOT NULL, `rateOverride` REAL, `serverId` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '55686f2add14e5061c7163282df7e6a8')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `shifts`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsShifts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsShifts.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("workOrderId", TableInfo.Column("workOrderId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("date", TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("startMinutes", TableInfo.Column("startMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("endMinutes", TableInfo.Column("endMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("breakMinutes", TableInfo.Column("breakMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("note", TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("billable", TableInfo.Column("billable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("rateOverride", TableInfo.Column("rateOverride", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShifts.put("serverId", TableInfo.Column("serverId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysShifts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesShifts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoShifts: TableInfo = TableInfo("shifts", _columnsShifts, _foreignKeysShifts, _indicesShifts)
        val _existingShifts: TableInfo = read(connection, "shifts")
        if (!_infoShifts.equals(_existingShifts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |shifts(dev.aliziwa.timesheet.data.local.ShiftEntity).
              | Expected:
              |""".trimMargin() + _infoShifts + """
              |
              | Found:
              |""".trimMargin() + _existingShifts)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "shifts")
  }

  public override fun clearAllTables() {
    super.performClear(false, "shifts")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(ShiftDao::class, ShiftDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    _autoMigrations.add(TimesheetDatabase_AutoMigration_1_2_Impl())
    return _autoMigrations
  }

  public override fun shiftDao(): ShiftDao = _shiftDao.value
}
