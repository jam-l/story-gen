package com.novelsim.app.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFileStorage(private val context: Context) : FileStorage {
    override suspend fun saveData(fileName: String, content: String) {
        withContext(Dispatchers.IO) {
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                it.write(content.toByteArray())
            }
        }
    }

    override suspend fun loadData(fileName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.openFileInput(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }
    }
}
