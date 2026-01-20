package com.novelsim.app.data.local

interface FileStorage {
    suspend fun saveData(fileName: String, content: String)
    suspend fun loadData(fileName: String): String?
}
