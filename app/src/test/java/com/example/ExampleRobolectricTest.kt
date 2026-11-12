package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SonicFlow", appName)
  }

  @Test
  fun `track formattedDuration handles zero and normal durations`() {
    val track1 = com.example.data.model.Track(
      id = 1,
      title = "Test",
      artist = "Artist",
      durationMs = 185000L,
      uriString = "content://test"
    )
    assertEquals("03:05", track1.formattedDuration())

    val trackZero = track1.copy(durationMs = 0L)
    assertEquals("--:--", trackZero.formattedDuration())
  }
}
