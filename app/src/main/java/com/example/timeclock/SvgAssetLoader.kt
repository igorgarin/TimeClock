package com.example.timeclock

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG
import java.util.zip.ZipInputStream

/**
 * Loads the original, editable SVG source artwork from the versioned design
 * archive. Keeping source vectors in one archive avoids lossy raster exports.
 */
object SvgAssetLoader {
    private const val ARCHIVE = "design/TimeClock_SVG_source.zip"

    fun load(context: Context, assetName: String): PictureDrawable {
        val svgText = ZipInputStream(context.assets.open(ARCHIVE)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == assetName) {
                    return@use zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            null
        } ?: error("SVG asset not found in design archive: $assetName")

        val svg = SVG.getFromString(svgText)
        return PictureDrawable(svg.renderToPicture())
    }
}
