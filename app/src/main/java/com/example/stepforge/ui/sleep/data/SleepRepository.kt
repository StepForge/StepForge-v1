package com.example.stepforge.ui.sleep.data

import com.example.stepforge.R
import com.example.stepforge.ui.sleep.model.*
import kotlin.math.sqrt

object SleepRepository {

    fun generateInsights(day: SleepDay): List<SleepInsight> = buildList {
        if (!day.hasAnyData) return@buildList

        val totalH = day.totalSleepMinutes / 60f
        val bedHour = day.bedTime?.hour

        if (bedHour != null && bedHour in 1..3) {
            add(
                SleepInsight(
                    titleRes = R.string.sleep_insight_late_bedtime_title,
                    bodyRes = R.string.sleep_insight_late_bedtime_body,
                    bodyArgs = arrayOf(day.bedTimeStr),
                    severity = InsightSeverity.WARNING
                )
            )
        }

        if (totalH < 6f) {
            add(
                SleepInsight(
                    titleRes = R.string.sleep_insight_insufficient_sleep_title,
                    bodyRes = R.string.sleep_insight_insufficient_sleep_body,
                    bodyArgs = arrayOf(day.hours, day.minutes),
                    severity = InsightSeverity.WARNING
                )
            )
        }

        day.bedTime?.let { bed ->
            val bedtimeMinutes = bed.hour * 60 + bed.minute
            if (bedtimeMinutes in 21 * 60..23 * 60) {
                add(
                    SleepInsight(
                        titleRes = R.string.sleep_insight_healthy_bedtime_title,
                        bodyRes = R.string.sleep_insight_healthy_bedtime_body,
                        severity = InsightSeverity.POSITIVE
                    )
                )
            }
        }

        if (day.hasFullData && day.totalSleepMinutes > 0) {
            day.remMinutes?.let { remMinutes ->
                val remR = remMinutes.toFloat() / day.totalSleepMinutes
                if (remR < 0.18f) {
                    add(
                        SleepInsight(
                            titleRes = R.string.sleep_insight_low_rem_title,
                            bodyRes = R.string.sleep_insight_low_rem_body,
                            bodyArgs = arrayOf((remR * 100).toInt()),
                            severity = InsightSeverity.INFO
                        )
                    )
                }
            }

            day.deepMinutes?.let { deepMinutes ->
                val deepR = deepMinutes.toFloat() / day.totalSleepMinutes
                if (deepR < 0.13f) {
                    add(
                        SleepInsight(
                            titleRes = R.string.sleep_insight_low_deep_title,
                            bodyRes = R.string.sleep_insight_low_deep_body,
                            bodyArgs = arrayOf((deepR * 100).toInt()),
                            severity = InsightSeverity.INFO
                        )
                    )
                }
            }
        }

        val score = day.sleepScore
        if (score != null && score >= 85) {
            add(
                SleepInsight(
                    titleRes = R.string.sleep_insight_great_night_title,
                    bodyRes = R.string.sleep_insight_great_night_body,
                    bodyArgs = arrayOf(score),
                    severity = InsightSeverity.POSITIVE
                )
            )
        }

        day.heartRateAvg?.let { hr ->
            if (hr < 58) {
                add(
                    SleepInsight(
                        titleRes = R.string.sleep_insight_rested_heart_title,
                        bodyRes = R.string.sleep_insight_rested_heart_body,
                        bodyArgs = arrayOf(hr),
                        severity = InsightSeverity.POSITIVE
                    )
                )
            }
        }
    }

    fun generateWeeklyConsistencyInsight(weekHistory: List<SleepDay>): SleepInsight? {
        val bedtimes = weekHistory.mapNotNull { it.bedTime }
        if (bedtimes.size < 4) return null

        val minutes = bedtimes.map { it.hour * 60 + it.minute }
        val avg = minutes.average()
        val variance = minutes.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)

        return if (stdDev < 30) {
            SleepInsight(
                titleRes = R.string.sleep_insight_consistent_schedule_title,
                bodyRes = R.string.sleep_insight_consistent_schedule_body,
                severity = InsightSeverity.POSITIVE
            )
        } else {
            null
        }
    }
}
