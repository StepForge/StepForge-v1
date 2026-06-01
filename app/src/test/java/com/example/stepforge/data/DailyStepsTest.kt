package com.example.stepforge.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStepsTest {

    @Test
    fun `test DailySteps creation with valid data`() {
        val dailySteps = DailySteps(date = "2023-10-01", steps = 10000)

        assertEquals("2023-10-01", dailySteps.date)
        assertEquals(10000, dailySteps.steps)
    }

    @Test
    fun `test DailySteps equality`() {
        val dailySteps1 = DailySteps(date = "2023-10-01", steps = 5000)
        val dailySteps2 = DailySteps(date = "2023-10-01", steps = 5000)

        assertEquals(dailySteps1, dailySteps2)
    }

    @Test
    fun `test DailySteps copy function`() {
        val original = DailySteps(date = "2023-10-01", steps = 8000)
        val copied = original.copy(steps = 9000)

        assertEquals("2023-10-01", copied.date)
        assertEquals(9000, copied.steps)
    }
}