package com.example.timeclock

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG
import java.util.zip.ZipInputStream

/** Loads original vector artwork without rasterizing it. */
object SvgAssetLoader {
    private const val ARCHIVE = "design/TimeClock_SVG_source.zip"
    private const val DESIGN_DIR = "design/"

    fun load(context: Context, assetName: String): PictureDrawable {
        val directSvg = runCatching {
            context.assets.open(DESIGN_DIR + assetName).bufferedReader().use { it.readText() }
        }.getOrNull()
        val svgText = directSvg ?: ZipInputStream(context.assets.open(ARCHIVE)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == assetName) {
                    return@use zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            null
        } ?: error("SVG asset not found: $assetName")
        return PictureDrawable(SVG.getFromString(svgText).renderToPicture())
    }
}
