package com.example.timeclock

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.caverock.androidsvg.SVG
import java.util.zip.ZipInputStream

/** Loads the SVG source artwork as vector PictureDrawables. */
object SvgAssetLoader {
    private const val ARCHIVE = "design/TimeClock_SVG_source.zip"
    private const val V2_CLAPPERBOARD = "design/clapperboard_V2_03.svg"

    fun load(context: Context, assetName: String): PictureDrawable {
        val svgText = if (assetName == "clapperboard_V2_03.svg") {
            context.assets.open(V2_CLAPPERBOARD).bufferedReader().use { it.readText() }
        } else {
            ZipInputStream(context.assets.open(ARCHIVE)).use { zip ->
                var entry = zip.nextEntry
                var result: String? = null
                while (entry != null) {
                    if (!entry.isDirectory && entry.name == assetName) {
                        result = zip.readBytes().toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                result
            } ?: error("SVG asset not found in design archive: $assetName")
        }
        return PictureDrawable(SVG.getFromString(svgText).renderToPicture())
    }
}
