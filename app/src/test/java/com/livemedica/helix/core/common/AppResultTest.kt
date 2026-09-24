package com.livemedica.helix.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppResultTest {

    @Test
    fun `map transforms success payload`() {
        val result: AppResult<Int> = AppResult.Success(2)
        assertEquals(AppResult.Success("2"), result.map { it.toString() })
    }

    @Test
    fun `map preserves offline cache so screens can keep showing stale data`() {
        val result: AppResult<Int> = AppResult.Offline(cached = 7)
        assertEquals(7, result.map { it }.dataOrNull())
    }

    @Test
    fun `failure exposes no data`() {
        val result: AppResult<Int> = AppResult.Failure("boom")
        assertNull(result.dataOrNull())
    }
}
