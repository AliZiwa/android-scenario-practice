package dev.aliziwa.dispatch.`data`.local

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
public class DispatchDatabase_Impl : DispatchDatabase() {
  private val _workOrderDao: Lazy<WorkOrderDao> = lazy {
    WorkOrderDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3, "1075318d523677b5838950d1d7d6cf91", "bc0d2f57cf56c5489b6095c43beb616c") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `work_orders` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `site` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `version` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isPending` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1075318d523677b5838950d1d7d6cf91')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `work_orders`")
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
        val _columnsWorkOrders: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWorkOrders.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("site", TableInfo.Column("site", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("priority", TableInfo.Column("priority", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("version", TableInfo.Column("version", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkOrders.put("isPending", TableInfo.Column("isPending", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWorkOrders: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWorkOrders: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWorkOrders: TableInfo = TableInfo("work_orders", _columnsWorkOrders, _foreignKeysWorkOrders, _indicesWorkOrders)
        val _existingWorkOrders: TableInfo = read(connection, "work_orders")
        if (!_infoWorkOrders.equals(_existingWorkOrders)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |work_orders(dev.aliziwa.dispatch.data.local.WorkOrderEntity).
              | Expected:
              |""".trimMargin() + _infoWorkOrders + """
              |
              | Found:
              |""".trimMargin() + _existingWorkOrders)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "work_orders")
  }

  public override fun clearAllTables() {
    super.performClear(false, "work_orders")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(WorkOrderDao::class, WorkOrderDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun workOrderDao(): WorkOrderDao = _workOrderDao.value
}
