package dev.aliziwa.timesheet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var workOrderId: String = "",
    var date: String = "", // yyyy-MM-dd
    var startMinutes: Int = 0, // minutes since midnight
    var endMinutes: Int = 0,
    var breakMinutes: Int = 0,
    var note: String? = null,
    var billable: Boolean = true,
    var status: String = "draft", // draft, submitted, approved
    var rateOverride: Double? = null,
    var serverId: String? = null,
    var tmp: String? = null, // TODO remove, was used for the old sync
)
