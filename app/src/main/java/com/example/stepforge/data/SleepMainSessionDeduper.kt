package com.example.stepforge.data

/**
 * Aynı takvim gününde birden fazla `type == main` kaydı oluşursa (Health Connect + otomatik uyku vb.)
 * tek bir ana uyku bırakır: öncelik manual > health_connect > auto; eşitlikte daha uzun süre.
 */
object SleepMainSessionDeduper {

    private val SOURCE_PRIORITY = mapOf(
        "manual" to 4,
        "health_connect" to 3,
        "auto" to 2
    )

    fun pickWinner(mains: List<SleepSession>): SleepSession {
        require(mains.isNotEmpty())
        return mains.maxWith(
            compareBy<SleepSession> { SOURCE_PRIORITY[it.source] ?: 1 }
                .thenBy { it.totalMinutes }
                .thenBy { it.endTime }
        )
    }

    suspend fun deduplicate(sessionDao: SleepSessionDao, stageDao: SleepStageDao) {
        val all = sessionDao.getRecentSessions(1000)
        val byDate = all.groupBy { it.date }
        for ((_, daySessions) in byDate) {
            val mains = daySessions.filter { it.type == "main" }
            if (mains.size <= 1) continue
            val keep = pickWinner(mains)
            for (m in mains) {
                if (m.id != keep.id) {
                    stageDao.deleteBySessionId(m.id)
                    sessionDao.deleteById(m.id)
                }
            }
        }
    }
}
