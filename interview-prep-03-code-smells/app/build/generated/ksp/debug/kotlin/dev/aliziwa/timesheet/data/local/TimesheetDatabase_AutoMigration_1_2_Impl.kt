package dev.aliziwa.timesheet.`data`.local

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Suppress

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
internal class TimesheetDatabase_AutoMigration_1_2_Impl : Migration {
  private val callback: AutoMigrationSpec = TimesheetDatabase.DeleteTmpSpec()

  public constructor() : super(1, 2)

  public override fun migrate(connection: SQLiteConnection) {
    connection.execSQL("CREATE TABLE IF NOT EXISTS `_new_shifts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workOrderId` TEXT NOT NULL, `date` TEXT NOT NULL, `startMinutes` INTEGER NOT NULL, `endMinutes` INTEGER NOT NULL, `breakMinutes` INTEGER NOT NULL, `note` TEXT, `billable` INTEGER NOT NULL, `status` TEXT NOT NULL, `rateOverride` REAL, `serverId` TEXT)")
    connection.execSQL("INSERT INTO `_new_shifts` (`id`,`workOrderId`,`date`,`startMinutes`,`endMinutes`,`breakMinutes`,`note`,`billable`,`status`,`rateOverride`,`serverId`) SELECT `id`,`workOrderId`,`date`,`startMinutes`,`endMinutes`,`breakMinutes`,`note`,`billable`,`status`,`rateOverride`,`serverId` FROM `shifts`")
    connection.execSQL("DROP TABLE `shifts`")
    connection.execSQL("ALTER TABLE `_new_shifts` RENAME TO `shifts`")
    callback.onPostMigrate(connection)
  }
}
