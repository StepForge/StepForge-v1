package com.example.stepforge.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyWaterTest {

    @Test
    fun `test DailyWater creation with valid data`() {
        val dailyWater = DailyWater(date = "2023-10-01", waterMl = 2000)

        assertEquals("2023-10-01", dailyWater.date)
        assertEquals(2000, dailyWater.waterMl)
    }

    @Test
    fun `test DailyWater equality`() {
        val dailyWater1 = DailyWater(date = "2023-10-01", waterMl = 1500)
        val dailyWater2 = DailyWater(date = "2023-10-01", waterMl = 1500)

        assertEquals(dailyWater1, dailyWater2)
    }

    @Test
    fun `test DailyWater copy function`() {
        val original = DailyWater(date = "2023-10-01", waterMl = 1000)
        val copied = original.copy(waterMl = 1200)

        assertEquals("2023-10-01", copied.date)
        assertEquals(1200, copied.waterMl)
    }
}