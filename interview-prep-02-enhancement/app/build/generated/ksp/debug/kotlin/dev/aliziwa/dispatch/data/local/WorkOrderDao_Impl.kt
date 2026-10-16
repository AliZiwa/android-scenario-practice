package dev.aliziwa.dispatch.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class WorkOrderDao_Impl(
  __db: RoomDatabase,
) : WorkOrderDao() {
  private val __db: RoomDatabase

  private val __upsertAdapterOfWorkOrderEntity: EntityUpsertAdapter<WorkOrderEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfWorkOrderEntity = EntityUpsertAdapter<WorkOrderEntity>(object : EntityInsertAdapter<WorkOrderEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `work_orders` (`id`,`title`,`site`,`priority`,`status`,`version`,`updatedAt`,`isPending`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WorkOrderEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.site)
        statement.bindText(4, entity.priority)
        statement.bindText(5, entity.status)
        statement.bindLong(6, entity.version.toLong())
        statement.bindLong(7, entity.updatedAt)
        val _tmp: Int = if (entity.isPending) 1 else 0
        statement.bindLong(8, _tmp.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<WorkOrderEntity>() {
      protected override fun createQuery(): String = "UPDATE `work_orders` SET `id` = ?,`title` = ?,`site` = ?,`priority` = ?,`status` = ?,`version` = ?,`updatedAt` = ?,`isPending` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: WorkOrderEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.site)
        statement.bindText(4, entity.priority)
        statement.bindText(5, entity.status)
        statement.bindLong(6, entity.version.toLong())
        statement.bindLong(7, entity.updatedAt)
        val _tmp: Int = if (entity.isPending) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.id)
      }
    })
  }

  public override suspend fun replaceAll(entities: List<WorkOrderEntity>): Unit = performInTransactionSuspending(__db) {
    super@WorkOrderDao_Impl.replaceAll(entities)
  }

  public override suspend fun upsertAll(entities: List<WorkOrderEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfWorkOrderEntity.upsert(_connection, entities)
  }

  public override suspend fun upsert(entity: WorkOrderEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfWorkOrderEntity.upsert(_connection, entity)
  }

  public override fun observeAll(): Flow<List<WorkOrderEntity>> {
    val _sql: String = "SELECT * FROM work_orders ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("work_orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSite: Int = getColumnIndexOrThrow(_stmt, "site")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfIsPending: Int = getColumnIndexOrThrow(_stmt, "isPending")
        val _result: MutableList<WorkOrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorkOrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSite: String
          _tmpSite = _stmt.getText(_columnIndexOfSite)
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpVersion: Int
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpIsPending: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPending).toInt()
          _tmpIsPending = _tmp != 0
          _item = WorkOrderEntity(_tmpId,_tmpTitle,_tmpSite,_tmpPriority,_tmpStatus,_tmpVersion,_tmpUpdatedAt,_tmpIsPending)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeById(id: String): Flow<WorkOrderEntity?> {
    val _sql: String = "SELECT * FROM work_orders WHERE id = ?"
    return createFlow(__db, false, arrayOf("work_orders")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSite: Int = getColumnIndexOrThrow(_stmt, "site")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfIsPending: Int = getColumnIndexOrThrow(_stmt, "isPending")
        val _result: WorkOrderEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSite: String
          _tmpSite = _stmt.getText(_columnIndexOfSite)
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpVersion: Int
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpIsPending: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPending).toInt()
          _tmpIsPending = _tmp != 0
          _result = WorkOrderEntity(_tmpId,_tmpTitle,_tmpSite,_tmpPriority,_tmpStatus,_tmpVersion,_tmpUpdatedAt,_tmpIsPending)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllOnce(): List<WorkOrderEntity> {
    val _sql: String = "SELECT * FROM work_orders"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSite: Int = getColumnIndexOrThrow(_stmt, "site")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfIsPending: Int = getColumnIndexOrThrow(_stmt, "isPending")
        val _result: MutableList<WorkOrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorkOrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSite: String
          _tmpSite = _stmt.getText(_columnIndexOfSite)
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpVersion: Int
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpIsPending: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPending).toInt()
          _tmpIsPending = _tmp != 0
          _item = WorkOrderEntity(_tmpId,_tmpTitle,_tmpSite,_tmpPriority,_tmpStatus,_tmpVersion,_tmpUpdatedAt,_tmpIsPending)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByIdOnce(id: String): WorkOrderEntity? {
    val _sql: String = "SELECT * FROM work_orders WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSite: Int = getColumnIndexOrThrow(_stmt, "site")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfIsPending: Int = getColumnIndexOrThrow(_stmt, "isPending")
        val _result: WorkOrderEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSite: String
          _tmpSite = _stmt.getText(_columnIndexOfSite)
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpVersion: Int
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpIsPending: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPending).toInt()
          _tmpIsPending = _tmp != 0
          _result = WorkOrderEntity(_tmpId,_tmpTitle,_tmpSite,_tmpPriority,_tmpStatus,_tmpVersion,_tmpUpdatedAt,_tmpIsPending)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPending(): List<WorkOrderEntity> {
    val _sql: String = "SELECT * FROM work_orders WHERE isPending = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfSite: Int = getColumnIndexOrThrow(_stmt, "site")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfVersion: Int = getColumnIndexOrThrow(_stmt, "version")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfIsPending: Int = getColumnIndexOrThrow(_stmt, "isPending")
        val _result: MutableList<WorkOrderEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorkOrderEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpSite: String
          _tmpSite = _stmt.getText(_columnIndexOfSite)
          val _tmpPriority: String
          _tmpPriority = _stmt.getText(_columnIndexOfPriority)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpVersion: Int
          _tmpVersion = _stmt.getLong(_columnIndexOfVersion).toInt()
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpIsPending: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsPending).toInt()
          _tmpIsPending = _tmp != 0
          _item = WorkOrderEntity(_tmpId,_tmpTitle,_tmpSite,_tmpPriority,_tmpStatus,_tmpVersion,_tmpUpdatedAt,_tmpIsPending)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAll() {
    val _sql: String = "DELETE FROM work_orders"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
