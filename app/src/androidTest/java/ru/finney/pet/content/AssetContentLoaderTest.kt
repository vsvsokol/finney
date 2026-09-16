package ru.finney.pet.content

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Контент читается из APK и проходит проверку на устройстве, включая Android 8.0. */
@RunWith(AndroidJUnit4::class)
class AssetContentLoaderTest {

    @Test
    fun contentLoadsFromAssets() {
        val content = AssetContentLoader(InstrumentationRegistry.getInstrumentation().targetContext).load()
        assertTrue(content.shop.isNotEmpty())
        assertTrue(content.tasks.isNotEmpty())
    }
}
