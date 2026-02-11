package com.example.stepforge.ui.insights


data class DayStepEntry(
    val date: String,      // "yyyy-MM-dd"
    val steps: Int
)

data class PeriodInsights(
    val startDate: String,
    val endDate: String,
    val totalSteps: Int,
    val dailyAverage: Int,
    val bestDayLabel: String,     // "Mon", "Tue"...
    val bestDaySteps: Int,
    val worstDayLabel: String,
    val worstDaySteps: Int,
    val activeDays: Int,          // days above threshold
    val peakHourLabel: String,    // "22:00 - 23:00"
    val consistencyScore: Int,    // 0..100
    val goalSuccess: Int,         // 0..periodDays
    val trendPercent: Int,        // -35 means -35%
    val trendLabel: String,       // "Up", "Down", "New"
    val summaryLines: List<String>,
    val chartData: List<DayStepEntry>
)



enum class InsightsMode {
    WEEKLY,
    MONTHLY
}

data class PremiumFeatureState(
    val isPremiumEnabled: Boolean
)
