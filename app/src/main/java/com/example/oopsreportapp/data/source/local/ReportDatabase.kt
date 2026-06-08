package com.example.oopsreportapp.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.oopsreportapp.data.source.local.dao.ReportDao
import com.example.oopsreportapp.data.source.local.entity.ReportEntity

@Database(entities = [ReportEntity::class], version = 2, exportSchema = false)
abstract class ReportDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: ReportDatabase? = null

        fun getInstance(context: Context): ReportDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReportDatabase::class.java,
                    "report_database"
                )
                .fallbackToDestructiveMigration() // Menghapus data lama jika struktur berubah (solusi crash)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
