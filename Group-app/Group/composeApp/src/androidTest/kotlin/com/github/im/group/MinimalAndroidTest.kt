package com.github.im.group

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 极其简单的Android Instrumentation测试
 * 只测试最基本的功能，排除所有复杂依赖
 */
@RunWith(AndroidJUnit4::class)
class MinimalAndroidTest {

    @Test
    fun testApplicationContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(appContext)
        assertEquals("com.github.im.group", appContext.packageName)
    }

    @Test
    fun testSimpleCalculation() {
        assertEquals(4, 2 + 2)
    }
}