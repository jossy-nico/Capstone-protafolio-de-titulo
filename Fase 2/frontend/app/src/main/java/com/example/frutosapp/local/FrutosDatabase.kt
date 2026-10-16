package com.example.frutosapp.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.frutosapp.model.CarritoInvitadoDao
import com.example.frutosapp.model.CarritoInvitadoEntity

@Database(
    entities = [CarritoInvitadoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FrutosDatabase : RoomDatabase() {

    abstract fun carritoInvitadoDao(): CarritoInvitadoDao

    companion object {

        @Volatile
        private var INSTANCE: FrutosDatabase? = null

        fun getDatabase(context: Context): FrutosDatabase {
            return INSTANCE ?: synchronized(this) {

                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    FrutosDatabase::class.java,
                    "frutos_database"
                ).build()

                INSTANCE = instancia
                instancia
            }
        }
    }
}