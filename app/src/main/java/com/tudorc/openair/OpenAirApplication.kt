package com.tudorc.openair

import android.app.Application
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import java.io.File

class OpenAirApplication : Application(), ImageLoaderFactory {
    override fun attachBaseContext(base: Context) {
        val attributed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            base.createAttributionContext(ATTRIBUTION_TAG)
        } else {
            base
        }
        super.attachBaseContext(attributed)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, IMAGE_CACHE_DIR))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
}

private const val ATTRIBUTION_TAG = "openair"
private const val IMAGE_CACHE_DIR = "station_icons"
