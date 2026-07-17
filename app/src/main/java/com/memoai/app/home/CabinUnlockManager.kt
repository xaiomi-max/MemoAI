package com.memoai.app.home

import android.content.SharedPreferences

data class CabinUnlockEvent(
    val completedTasks: Int,
    val furnitureName: String
)

object CabinUnlockManager {
    val totalFurnitureCount = 50

    private val furnitureUnlocks = listOf(
        1 to "小地毯",
        3 to "台灯",
        5 to "新椅子",
        8 to "抱枕",
        10 to "书架",
        12 to "窗帘",
        15 to "新桌子",
        18 to "花瓶",
        20 to "绿植",
        25 to "沙发",
        30 to "挂画",
        35 to "地毯",
        40 to "吊灯",
        45 to "猫爬架",
        50 to "理想家园"
    )

    fun getUnlockedCount(completedTasks: Int): Int =
        furnitureUnlocks.count { it.first <= completedTasks }

    fun syncUnlockedCount(prefs: SharedPreferences, completedTasks: Int) {
        prefs.edit()
            .putInt("cabin_unlocked_furniture", getUnlockedCount(completedTasks))
            .putInt("completed_task_count", completedTasks)
            .apply()
    }

    fun getNextUnlock(completedTasks: Int): Pair<Int, String>? =
        furnitureUnlocks.firstOrNull { it.first > completedTasks }

    fun getStoredCompletedTaskCount(prefs: SharedPreferences): Int =
        prefs.getInt("completed_task_count", 0)

    fun getStoredUnlockedCount(prefs: SharedPreferences): Int =
        prefs.getInt("cabin_unlocked_furniture", 0)

    fun onTaskCompleted(prefs: SharedPreferences, completedTasks: Int): CabinUnlockEvent? {
        val unlock = furnitureUnlocks.find { it.first == completedTasks }
        if (unlock == null) return null
        val announced = prefs.getStringSet(PREF_ANNOUNCED_UNLOCKS, emptySet()) ?: emptySet()
        val key = unlock.first.toString()
        if (announced.contains(key)) return null
        prefs.edit()
            .putStringSet(PREF_ANNOUNCED_UNLOCKS, announced + key)
            .putInt("cabin_unlocked_furniture", getUnlockedCount(completedTasks))
            .apply()
        return CabinUnlockEvent(completedTasks, unlock.second)
    }

    private const val PREF_ANNOUNCED_UNLOCKS = "cabin_announced_unlocks"
}
