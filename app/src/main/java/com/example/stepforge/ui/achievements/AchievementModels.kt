package com.example.stepforge.ui.achievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.stepforge.R

internal enum class AchievementCategory {
    ALL,
    STEPS,
    DISTANCE,
    CALORIES,
    STREAKS,
    WORKOUTS,
    TIME,
    WEATHER,
    SPECIAL
}

internal enum class AchievementRarity(val points: Int) {
    COMMON(100),
    UNCOMMON(250),
    RARE(500),
    EPIC(900),
    LEGENDARY(1500),
    MYTHIC(2500)
}

internal data class AchievementDefinition(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    val target: Float
)

@StringRes
internal fun AchievementDefinition.unlockHintRes(): Int = when (id) {
    "first_steps" -> R.string.ach_unlock_01
    "daily_goal" -> R.string.ach_unlock_02
    "power_day" -> R.string.ach_unlock_03
    "monster_walk" -> R.string.ach_unlock_04
    "total_steps_bronze" -> R.string.ach_unlock_05
    "total_steps_silver" -> R.string.ach_unlock_06
    "total_steps_gold" -> R.string.ach_unlock_07
    "million_steps" -> R.string.ach_unlock_08
    "distance_explorer" -> R.string.ach_unlock_09
    "city_walker" -> R.string.ach_unlock_10
    "long_road" -> R.string.ach_unlock_11
    "marathon_spirit" -> R.string.ach_unlock_12
    "distance_master" -> R.string.ach_unlock_13
    "first_burn" -> R.string.ach_unlock_14
    "calorie_burner" -> R.string.ach_unlock_15
    "fire_engine" -> R.string.ach_unlock_16
    "inferno_day" -> R.string.ach_unlock_17
    "active_hour" -> R.string.ach_unlock_18
    "two_hour_push" -> R.string.ach_unlock_19
    "endurance_pro" -> R.string.ach_unlock_20
    "first_workout" -> R.string.ach_unlock_21
    "workout_builder" -> R.string.ach_unlock_22
    "workout_machine" -> R.string.ach_unlock_23
    "walker" -> R.string.ach_unlock_24
    "runner" -> R.string.ach_unlock_25
    "cyclist" -> R.string.ach_unlock_26
    "streak_spark" -> R.string.ach_unlock_27
    "week_streak" -> R.string.ach_unlock_28
    "month_streak" -> R.string.ach_unlock_29
    "unbreakable_streak" -> R.string.ach_unlock_30
    "streak_shield" -> R.string.ach_unlock_31
    "second_chance" -> R.string.ach_unlock_32
    "building_week" -> R.string.ach_unlock_33
    "strong_week" -> R.string.ach_unlock_34
    "perfect_week" -> R.string.ach_unlock_35
    "active_month" -> R.string.ach_unlock_36
    "elite_month" -> R.string.ach_unlock_37
    "goal_hunter" -> R.string.ach_unlock_38
    "goal_machine" -> R.string.ach_unlock_39
    "goal_master" -> R.string.ach_unlock_40
    "early_bird" -> R.string.ach_unlock_41
    "night_walker" -> R.string.ach_unlock_42
    "weekend_warrior" -> R.string.ach_unlock_43
    "rain_walker" -> R.string.ach_unlock_44
    "snow_walker" -> R.string.ach_unlock_45
    "comeback" -> R.string.ach_unlock_46
    "new_record" -> R.string.ach_unlock_47
    "top_day" -> R.string.ach_unlock_48
    "weekly_champion" -> R.string.ach_unlock_49
    "legendary_walker" -> R.string.ach_unlock_50
    else -> R.string.ach_unlock_generic
}


internal data class AchievementItemUi(
    val definition: AchievementDefinition,
    val progress: Float,
    val current: Float,
    val unlocked: Boolean
)

internal data class AchievementCategorySummaryUi(
    val category: AchievementCategory,
    val unlocked: Int,
    val total: Int
)

internal data class AchievementsUiState(
    val achievements: List<AchievementItemUi>,
    val unlockedCount: Int,
    val totalCount: Int,
    val completion: Float,
    val totalPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val bestAchievement: AchievementItemUi?,
    val nextTarget: AchievementItemUi?,
    val recentUnlocked: List<AchievementItemUi>,
    val nextTargets: List<AchievementItemUi>,
    val categorySummaries: List<AchievementCategorySummaryUi>
) {
    companion object {
        fun empty(): AchievementsUiState = AchievementsUiState(
            achievements = emptyList(),
            unlockedCount = 0,
            totalCount = 0,
            completion = 0f,
            totalPoints = 0,
            currentStreak = 0,
            longestStreak = 0,
            bestAchievement = null,
            nextTarget = null,
            recentUnlocked = emptyList(),
            nextTargets = emptyList(),
            categorySummaries = emptyList()
        )
    }
}

internal val achievementDefinitions = listOf(
    AchievementDefinition("first_steps", R.string.ach_title_01, R.string.ach_desc_01, R.drawable.ach_01_first_steps, AchievementCategory.STEPS, AchievementRarity.COMMON, 1f),
    AchievementDefinition("daily_goal", R.string.ach_title_02, R.string.ach_desc_02, R.drawable.ach_02_daily_goal, AchievementCategory.STREAKS, AchievementRarity.COMMON, 1f),
    AchievementDefinition("power_day", R.string.ach_title_03, R.string.ach_desc_03, R.drawable.ach_03_power_day, AchievementCategory.STEPS, AchievementRarity.RARE, 20_000f),
    AchievementDefinition("monster_walk", R.string.ach_title_04, R.string.ach_desc_04, R.drawable.ach_04_monster_walk, AchievementCategory.STEPS, AchievementRarity.EPIC, 30_000f),
    AchievementDefinition("total_steps_bronze", R.string.ach_title_05, R.string.ach_desc_05, R.drawable.ach_05_total_steps_bronze, AchievementCategory.STEPS, AchievementRarity.UNCOMMON, 50_000f),
    AchievementDefinition("total_steps_silver", R.string.ach_title_06, R.string.ach_desc_06, R.drawable.ach_06_total_steps_silver, AchievementCategory.STEPS, AchievementRarity.RARE, 250_000f),
    AchievementDefinition("total_steps_gold", R.string.ach_title_07, R.string.ach_desc_07, R.drawable.ach_07_total_steps_gold, AchievementCategory.STEPS, AchievementRarity.EPIC, 500_000f),
    AchievementDefinition("million_steps", R.string.ach_title_08, R.string.ach_desc_08, R.drawable.ach_08_million_steps, AchievementCategory.STEPS, AchievementRarity.LEGENDARY, 1_000_000f),
    AchievementDefinition("distance_explorer", R.string.ach_title_09, R.string.ach_desc_09, R.drawable.ach_09_distance_explorer, AchievementCategory.DISTANCE, AchievementRarity.COMMON, 5f),
    AchievementDefinition("city_walker", R.string.ach_title_10, R.string.ach_desc_10, R.drawable.ach_10_city_walker, AchievementCategory.DISTANCE, AchievementRarity.UNCOMMON, 25f),
    AchievementDefinition("long_road", R.string.ach_title_11, R.string.ach_desc_11, R.drawable.ach_11_long_road, AchievementCategory.DISTANCE, AchievementRarity.RARE, 100f),
    AchievementDefinition("marathon_spirit", R.string.ach_title_12, R.string.ach_desc_12, R.drawable.ach_12_marathon_spirit, AchievementCategory.DISTANCE, AchievementRarity.EPIC, 42.2f),
    AchievementDefinition("distance_master", R.string.ach_title_13, R.string.ach_desc_13, R.drawable.ach_13_distance_master, AchievementCategory.DISTANCE, AchievementRarity.LEGENDARY, 1_000f),
    AchievementDefinition("first_burn", R.string.ach_title_14, R.string.ach_desc_14, R.drawable.ach_14_first_burn, AchievementCategory.CALORIES, AchievementRarity.COMMON, 1_000f),
    AchievementDefinition("calorie_burner", R.string.ach_title_15, R.string.ach_desc_15, R.drawable.ach_15_calorie_burner, AchievementCategory.CALORIES, AchievementRarity.UNCOMMON, 5_000f),
    AchievementDefinition("fire_engine", R.string.ach_title_16, R.string.ach_desc_16, R.drawable.ach_16_fire_engine, AchievementCategory.CALORIES, AchievementRarity.RARE, 50_000f),
    AchievementDefinition("inferno_day", R.string.ach_title_17, R.string.ach_desc_17, R.drawable.ach_17_inferno_day, AchievementCategory.CALORIES, AchievementRarity.EPIC, 1_000f),
    AchievementDefinition("active_hour", R.string.ach_title_18, R.string.ach_desc_18, R.drawable.ach_18_active_hour, AchievementCategory.TIME, AchievementRarity.COMMON, 60f),
    AchievementDefinition("two_hour_push", R.string.ach_title_19, R.string.ach_desc_19, R.drawable.ach_19_two_hour_push, AchievementCategory.TIME, AchievementRarity.UNCOMMON, 120f),
    AchievementDefinition("endurance_pro", R.string.ach_title_20, R.string.ach_desc_20, R.drawable.ach_20_endurance_pro, AchievementCategory.TIME, AchievementRarity.RARE, 6_000f),
    AchievementDefinition("first_workout", R.string.ach_title_21, R.string.ach_desc_21, R.drawable.ach_21_first_workout, AchievementCategory.WORKOUTS, AchievementRarity.COMMON, 1f),
    AchievementDefinition("workout_builder", R.string.ach_title_22, R.string.ach_desc_22, R.drawable.ach_22_workout_builder, AchievementCategory.WORKOUTS, AchievementRarity.UNCOMMON, 10f),
    AchievementDefinition("workout_machine", R.string.ach_title_23, R.string.ach_desc_23, R.drawable.ach_23_workout_machine, AchievementCategory.WORKOUTS, AchievementRarity.EPIC, 50f),
    AchievementDefinition("walker", R.string.ach_title_24, R.string.ach_desc_24, R.drawable.ach_24_walker, AchievementCategory.WORKOUTS, AchievementRarity.UNCOMMON, 10f),
    AchievementDefinition("runner", R.string.ach_title_25, R.string.ach_desc_25, R.drawable.ach_25_runner, AchievementCategory.WORKOUTS, AchievementRarity.RARE, 10f),
    AchievementDefinition("cyclist", R.string.ach_title_26, R.string.ach_desc_26, R.drawable.ach_26_cyclist, AchievementCategory.WORKOUTS, AchievementRarity.RARE, 10f),
    AchievementDefinition("streak_spark", R.string.ach_title_27, R.string.ach_desc_27, R.drawable.ach_27_streak_spark, AchievementCategory.STREAKS, AchievementRarity.COMMON, 3f),
    AchievementDefinition("week_streak", R.string.ach_title_28, R.string.ach_desc_28, R.drawable.ach_28_week_streak, AchievementCategory.STREAKS, AchievementRarity.UNCOMMON, 7f),
    AchievementDefinition("month_streak", R.string.ach_title_29, R.string.ach_desc_29, R.drawable.ach_29_month_streak, AchievementCategory.STREAKS, AchievementRarity.EPIC, 30f),
    AchievementDefinition("unbreakable_streak", R.string.ach_title_30, R.string.ach_desc_30, R.drawable.ach_30_unbreakable_streak, AchievementCategory.STREAKS, AchievementRarity.LEGENDARY, 100f),
    AchievementDefinition("streak_shield", R.string.ach_title_31, R.string.ach_desc_31, R.drawable.ach_31_streak_shield, AchievementCategory.STREAKS, AchievementRarity.RARE, 1f),
    AchievementDefinition("second_chance", R.string.ach_title_32, R.string.ach_desc_32, R.drawable.ach_32_second_chance, AchievementCategory.STREAKS, AchievementRarity.RARE, 1f),
    AchievementDefinition("building_week", R.string.ach_title_33, R.string.ach_desc_33, R.drawable.ach_33_building_week, AchievementCategory.STREAKS, AchievementRarity.COMMON, 3f),
    AchievementDefinition("strong_week", R.string.ach_title_34, R.string.ach_desc_34, R.drawable.ach_34_strong_week, AchievementCategory.STREAKS, AchievementRarity.UNCOMMON, 5f),
    AchievementDefinition("perfect_week", R.string.ach_title_35, R.string.ach_desc_35, R.drawable.ach_35_perfect_week, AchievementCategory.STREAKS, AchievementRarity.RARE, 7f),
    AchievementDefinition("active_month", R.string.ach_title_36, R.string.ach_desc_36, R.drawable.ach_36_active_month, AchievementCategory.STREAKS, AchievementRarity.UNCOMMON, 15f),
    AchievementDefinition("elite_month", R.string.ach_title_37, R.string.ach_desc_37, R.drawable.ach_37_elite_month, AchievementCategory.STREAKS, AchievementRarity.EPIC, 25f),
    AchievementDefinition("goal_hunter", R.string.ach_title_38, R.string.ach_desc_38, R.drawable.ach_38_goal_hunter, AchievementCategory.STREAKS, AchievementRarity.UNCOMMON, 10f),
    AchievementDefinition("goal_machine", R.string.ach_title_39, R.string.ach_desc_39, R.drawable.ach_39_goal_machine, AchievementCategory.STREAKS, AchievementRarity.RARE, 50f),
    AchievementDefinition("goal_master", R.string.ach_title_40, R.string.ach_desc_40, R.drawable.ach_40_goal_master, AchievementCategory.STREAKS, AchievementRarity.LEGENDARY, 100f),
    AchievementDefinition("early_bird", R.string.ach_title_41, R.string.ach_desc_41, R.drawable.ach_41_early_bird, AchievementCategory.SPECIAL, AchievementRarity.RARE, 10f),
    AchievementDefinition("night_walker", R.string.ach_title_42, R.string.ach_desc_42, R.drawable.ach_42_night_walker, AchievementCategory.SPECIAL, AchievementRarity.RARE, 10f),
    AchievementDefinition("weekend_warrior", R.string.ach_title_43, R.string.ach_desc_43, R.drawable.ach_43_weekend_warrior, AchievementCategory.SPECIAL, AchievementRarity.EPIC, 10f),
    AchievementDefinition("rain_walker", R.string.ach_title_44, R.string.ach_desc_44, R.drawable.ach_44_rain_walker, AchievementCategory.WEATHER, AchievementRarity.RARE, 1f),
    AchievementDefinition("snow_walker", R.string.ach_title_45, R.string.ach_desc_45, R.drawable.ach_45_snow_walker, AchievementCategory.WEATHER, AchievementRarity.RARE, 1f),
    AchievementDefinition("comeback", R.string.ach_title_46, R.string.ach_desc_46, R.drawable.ach_46_comeback, AchievementCategory.SPECIAL, AchievementRarity.RARE, 1f),
    AchievementDefinition("new_record", R.string.ach_title_47, R.string.ach_desc_47, R.drawable.ach_47_new_record, AchievementCategory.SPECIAL, AchievementRarity.UNCOMMON, 1f),
    AchievementDefinition("top_day", R.string.ach_title_48, R.string.ach_desc_48, R.drawable.ach_48_top_day, AchievementCategory.SPECIAL, AchievementRarity.RARE, 1f),
    AchievementDefinition("weekly_champion", R.string.ach_title_49, R.string.ach_desc_49, R.drawable.ach_49_weekly_champion, AchievementCategory.SPECIAL, AchievementRarity.EPIC, 1f),
    AchievementDefinition("legendary_walker", R.string.ach_title_50, R.string.ach_desc_50, R.drawable.ach_50_legendary_walker, AchievementCategory.STEPS, AchievementRarity.MYTHIC, 5_000_000f)
)
