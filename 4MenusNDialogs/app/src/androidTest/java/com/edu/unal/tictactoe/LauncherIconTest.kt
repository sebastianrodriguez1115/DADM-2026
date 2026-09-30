package com.edu.unal.tictactoe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val expectedBackground: Int
        get() = context.getColor(R.color.tic_tac_toe_icon_background)

    @Test
    fun adaptiveMipmapsResolveAndRenderOwnMotif() {
        val ids = listOf(
            "normal" to R.mipmap.ic_launcher,
            "round" to R.mipmap.ic_launcher_round,
        )
        ids.forEach { (label, id) ->
            val icon = requireNotNull(context.getDrawable(id) as? AdaptiveIconDrawable) {
                "$label must resolve AdaptiveIconDrawable"
            }.mutate() as AdaptiveIconDrawable

            val background = icon.background
            assertTrue("$label background must be ColorDrawable", background is ColorDrawable)
            assertEquals(expectedBackground, (background as ColorDrawable).color)
            assertNotNull("$label foreground", icon.foreground)
            assertNotNull("$label monochrome", icon.monochrome)

            val bitmap = render(icon, RENDER_SIZE)
            savePng(bitmap, "preview_$label.png")

            val palette = pixelColors(bitmap)
            assertTrue("$label must show X green", palette.contains(X_GREEN))
            assertTrue("$label must show O red", palette.contains(O_RED))
            assertTrue("$label must show own background", palette.contains(expectedBackground))
        }
    }

    @Test
    fun monochromeLayerRendersWhiteForegroundOverBackground() {
        val original = requireNotNull(
            context.getDrawable(R.mipmap.ic_launcher) as? AdaptiveIconDrawable,
        ) {
            "ic_launcher must resolve AdaptiveIconDrawable"
        }.mutate() as AdaptiveIconDrawable

        val normalProfile = alphaProfile(render(original, RENDER_SIZE))
        val monochrome = AdaptiveIconDrawable(
            ColorDrawable(expectedBackground),
            original.monochrome!!.mutate().apply { setTint(Color.WHITE) },
        )

        val bitmap = render(monochrome, RENDER_SIZE)
        savePng(bitmap, "preview_monochrome.png")

        val palette = pixelColors(bitmap)
        assertTrue("monochrome must show white foreground", palette.contains(Color.WHITE))
        assertTrue("monochrome must keep own background", palette.contains(expectedBackground))
        assertEquals(
            "monochrome transparency must match the system mask",
            normalProfile,
            alphaProfile(bitmap),
        )
    }

    private fun render(drawable: Drawable, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun pixelColors(bitmap: Bitmap): Set<Int> = bitmap.readPixels().toSet()

    private fun alphaProfile(bitmap: Bitmap): Pair<Int, Int> {
        var min = 255
        var zeros = 0
        for (pixel in bitmap.readPixels()) {
            val alpha = Color.alpha(pixel)
            if (alpha < min) min = alpha
            if (alpha == 0) zeros++
        }
        return min to zeros
    }

    private fun Bitmap.readPixels(): IntArray {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        return pixels
    }

    private fun savePng(bitmap: Bitmap, name: String) {
        val dir = File(
            context.getExternalFilesDir(null) ?: context.cacheDir,
            "icon-verification",
        )
        assertTrue("cannot create output dir $dir", dir.exists() || dir.mkdirs())
        File(dir, name).outputStream().use { output ->
            assertTrue(
                "failed to write $name",
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output),
            )
        }
    }

    private companion object {
        const val RENDER_SIZE = 192
        const val X_GREEN = 0xFF80DA95.toInt()
        const val O_RED = 0xFFFFB4AB.toInt()
    }
}
