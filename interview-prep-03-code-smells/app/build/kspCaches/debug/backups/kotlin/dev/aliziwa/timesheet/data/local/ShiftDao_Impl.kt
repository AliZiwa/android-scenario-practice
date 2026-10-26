package dev.aliziwa.timesheet.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
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
public class ShiftDao_Impl(
  __db: RoomDatabase,
) : ShiftDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfShiftEntity: EntityInsertAdapter<ShiftEntity>

  private val __updateAdapterOfShiftEntity: EntityDeleteOrUpdateAdapter<ShiftEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfShiftEntity = object : EntityInsertAdapter<ShiftEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `shifts` (`id`,`workOrderId`,`date`,`startMinutes`,`endMinutes`,`breakMinutes`,`note`,`billable`,`status`,`rateOverride`,`serverId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ShiftEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.workOrderId)
        statement.bindText(3, entity.date)
        statement.bindLong(4, entity.startMinutes.toLong())
        statement.bindLong(5, entity.endMinutes.toLong())
        statement.bindLong(6, entity.breakMinutes.toLong())
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpNote)
        }
        val _tmp: Int = if (entity.billable) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.status)
        val _tmpRateOverride: Double? = entity.rateOverride
        if (_tmpRateOverride == null) {
          statement.bindNull(10)
        } else {
          statement.bindDouble(10, _tmpRateOverride)
        }
        val _tmpServerId: String? = entity.serverId
        if (_tmpServerId == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpServerId)
        }
      }
    }
    this.__updateAdapterOfShiftEntity = object : EntityDeleteOrUpdateAdapter<ShiftEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `shifts` SET `id` = ?,`workOrderId` = ?,`date` = ?,`startMinutes` = ?,`endMinutes` = ?,`breakMinutes` = ?,`note` = ?,`billable` = ?,`status` = ?,`rateOverride` = ?,`serverId` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ShiftEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.workOrderId)
        statement.bindText(3, entity.date)
        statement.bindLong(4, entity.startMinutes.toLong())
        statement.bindLong(5, entity.endMinutes.toLong())
        statement.bindLong(6, entity.breakMinutes.toLong())
        val _tmpNote: String? = entity.note
        if (_tmpNote == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpNote)
        }
        val _tmp: Int = if (entity.billable) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        statement.bindText(9, entity.status)
        val _tmpRateOverride: Double? = entity.rateOverride
        if (_tmpRateOverride == null) {
          statement.bindNull(10)
        } else {
          statement.bindDouble(10, _tmpRateOverride)
        }
        val _tmpServerId: String? = entity.serverId
        if (_tmpServerId == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpServerId)
        }
        statement.bindLong(12, entity.id)
      }
    }
  }

  public override suspend fun insert(shift: ShiftEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfShiftEntity.insertAndReturnId(_connection, shift)
    _result
  }

  public override suspend fun update(shift: ShiftEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfShiftEntity.handle(_connection, shift)
  }

  public override fun observeAll(): Flow<List<ShiftEntity>> {
    val _sql: String = "SELECT * FROM shifts ORDER BY date DESC, startMinutes DESC"
    return createFlow(__db, false, arrayOf("shifts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfWorkOrderId: Int = getColumnIndexOrThrow(_stmt, "workOrderId")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfStartMinutes: Int = getColumnIndexOrThrow(_stmt, "startMinutes")
        val _columnIndexOfEndMinutes: Int = getColumnIndexOrThrow(_stmt, "endMinutes")
        val _columnIndexOfBreakMinutes: Int = getColumnIndexOrThrow(_stmt, "breakMinutes")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfBillable: Int = getColumnIndexOrThrow(_stmt, "billable")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfRateOverride: Int = getColumnIndexOrThrow(_stmt, "rateOverride")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _result: MutableList<ShiftEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShiftEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpWorkOrderId: String
          _tmpWorkOrderId = _stmt.getText(_columnIndexOfWorkOrderId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpStartMinutes: Int
          _tmpStartMinutes = _stmt.getLong(_columnIndexOfStartMinutes).toInt()
          val _tmpEndMinutes: Int
          _tmpEndMinutes = _stmt.getLong(_columnIndexOfEndMinutes).toInt()
          val _tmpBreakMinutes: Int
          _tmpBreakMinutes = _stmt.getLong(_columnIndexOfBreakMinutes).toInt()
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpBillable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBillable).toInt()
          _tmpBillable = _tmp != 0
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpRateOverride: Double?
          if (_stmt.isNull(_columnIndexOfRateOverride)) {
            _tmpRateOverride = null
          } else {
            _tmpRateOverride = _stmt.getDouble(_columnIndexOfRateOverride)
          }
          val _tmpServerId: String?
          if (_stmt.isNull(_columnIndexOfServerId)) {
            _tmpServerId = null
          } else {
            _tmpServerId = _stmt.getText(_columnIndexOfServerId)
          }
          _item = ShiftEntity(_tmpId,_tmpWorkOrderId,_tmpDate,_tmpStartMinutes,_tmpEndMinutes,_tmpBreakMinutes,_tmpNote,_tmpBillable,_tmpStatus,_tmpRateOverride,_tmpServerId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAll(): List<ShiftEntity> {
    val _sql: String = "SELECT * FROM shifts ORDER BY date DESC, startMinutes DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfWorkOrderId: Int = getColumnIndexOrThrow(_stmt, "workOrderId")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfStartMinutes: Int = getColumnIndexOrThrow(_stmt, "startMinutes")
        val _columnIndexOfEndMinutes: Int = getColumnIndexOrThrow(_stmt, "endMinutes")
        val _columnIndexOfBreakMinutes: Int = getColumnIndexOrThrow(_stmt, "breakMinutes")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfBillable: Int = getColumnIndexOrThrow(_stmt, "billable")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfRateOverride: Int = getColumnIndexOrThrow(_stmt, "rateOverride")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _result: MutableList<ShiftEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShiftEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpWorkOrderId: String
          _tmpWorkOrderId = _stmt.getText(_columnIndexOfWorkOrderId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpStartMinutes: Int
          _tmpStartMinutes = _stmt.getLong(_columnIndexOfStartMinutes).toInt()
          val _tmpEndMinutes: Int
          _tmpEndMinutes = _stmt.getLong(_columnIndexOfEndMinutes).toInt()
          val _tmpBreakMinutes: Int
          _tmpBreakMinutes = _stmt.getLong(_columnIndexOfBreakMinutes).toInt()
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpBillable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBillable).toInt()
          _tmpBillable = _tmp != 0
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpRateOverride: Double?
          if (_stmt.isNull(_columnIndexOfRateOverride)) {
            _tmpRateOverride = null
          } else {
            _tmpRateOverride = _stmt.getDouble(_columnIndexOfRateOverride)
          }
          val _tmpServerId: String?
          if (_stmt.isNull(_columnIndexOfServerId)) {
            _tmpServerId = null
          } else {
            _tmpServerId = _stmt.getText(_columnIndexOfServerId)
          }
          _item = ShiftEntity(_tmpId,_tmpWorkOrderId,_tmpDate,_tmpStartMinutes,_tmpEndMinutes,_tmpBreakMinutes,_tmpNote,_tmpBillable,_tmpStatus,_tmpRateOverride,_tmpServerId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getById(id: Long): ShiftEntity? {
    val _sql: String = "SELECT * FROM shifts WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfWorkOrderId: Int = getColumnIndexOrThrow(_stmt, "workOrderId")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfStartMinutes: Int = getColumnIndexOrThrow(_stmt, "startMinutes")
        val _columnIndexOfEndMinutes: Int = getColumnIndexOrThrow(_stmt, "endMinutes")
        val _columnIndexOfBreakMinutes: Int = getColumnIndexOrThrow(_stmt, "breakMinutes")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfBillable: Int = getColumnIndexOrThrow(_stmt, "billable")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfRateOverride: Int = getColumnIndexOrThrow(_stmt, "rateOverride")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _result: ShiftEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpWorkOrderId: String
          _tmpWorkOrderId = _stmt.getText(_columnIndexOfWorkOrderId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpStartMinutes: Int
          _tmpStartMinutes = _stmt.getLong(_columnIndexOfStartMinutes).toInt()
          val _tmpEndMinutes: Int
          _tmpEndMinutes = _stmt.getLong(_columnIndexOfEndMinutes).toInt()
          val _tmpBreakMinutes: Int
          _tmpBreakMinutes = _stmt.getLong(_columnIndexOfBreakMinutes).toInt()
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpBillable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBillable).toInt()
          _tmpBillable = _tmp != 0
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpRateOverride: Double?
          if (_stmt.isNull(_columnIndexOfRateOverride)) {
            _tmpRateOverride = null
          } else {
            _tmpRateOverride = _stmt.getDouble(_columnIndexOfRateOverride)
          }
          val _tmpServerId: String?
          if (_stmt.isNull(_columnIndexOfServerId)) {
            _tmpServerId = null
          } else {
            _tmpServerId = _stmt.getText(_columnIndexOfServerId)
          }
          _result = ShiftEntity(_tmpId,_tmpWorkOrderId,_tmpDate,_tmpStartMinutes,_tmpEndMinutes,_tmpBreakMinutes,_tmpNote,_tmpBillable,_tmpStatus,_tmpRateOverride,_tmpServerId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getByStatus(status: String): List<ShiftEntity> {
    val _sql: String = "SELECT * FROM shifts WHERE status = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, status)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfWorkOrderId: Int = getColumnIndexOrThrow(_stmt, "workOrderId")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfStartMinutes: Int = getColumnIndexOrThrow(_stmt, "startMinutes")
        val _columnIndexOfEndMinutes: Int = getColumnIndexOrThrow(_stmt, "endMinutes")
        val _columnIndexOfBreakMinutes: Int = getColumnIndexOrThrow(_stmt, "breakMinutes")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfBillable: Int = getColumnIndexOrThrow(_stmt, "billable")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfRateOverride: Int = getColumnIndexOrThrow(_stmt, "rateOverride")
        val _columnIndexOfServerId: Int = getColumnIndexOrThrow(_stmt, "serverId")
        val _result: MutableList<ShiftEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShiftEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpWorkOrderId: String
          _tmpWorkOrderId = _stmt.getText(_columnIndexOfWorkOrderId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpStartMinutes: Int
          _tmpStartMinutes = _stmt.getLong(_columnIndexOfStartMinutes).toInt()
          val _tmpEndMinutes: Int
          _tmpEndMinutes = _stmt.getLong(_columnIndexOfEndMinutes).toInt()
          val _tmpBreakMinutes: Int
          _tmpBreakMinutes = _stmt.getLong(_columnIndexOfBreakMinutes).toInt()
          val _tmpNote: String?
          if (_stmt.isNull(_columnIndexOfNote)) {
            _tmpNote = null
          } else {
            _tmpNote = _stmt.getText(_columnIndexOfNote)
          }
          val _tmpBillable: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfBillable).toInt()
          _tmpBillable = _tmp != 0
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpRateOverride: Double?
          if (_stmt.isNull(_columnIndexOfRateOverride)) {
            _tmpRateOverride = null
          } else {
            _tmpRateOverride = _stmt.getDouble(_columnIndexOfRateOverride)
          }
          val _tmpServerId: String?
          if (_stmt.isNull(_columnIndexOfServerId)) {
            _tmpServerId = null
          } else {
            _tmpServerId = _stmt.getText(_columnIndexOfServerId)
          }
          _item = ShiftEntity(_tmpId,_tmpWorkOrderId,_tmpDate,_tmpStartMinutes,_tmpEndMinutes,_tmpBreakMinutes,_tmpNote,_tmpBillable,_tmpStatus,_tmpRateOverride,_tmpServerId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM shifts WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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
