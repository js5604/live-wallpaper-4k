package com.wallpaper.live.themes

import android.content.Context
import android.content.SharedPreferences

enum class WallpaperTheme(val id: String, val title: String, val description: String) {
    KOI_POND(
        "koi_pond",
        "4K Koi Water Ripple",
        "Interactive swimming koi fish that scatter when you touch the water ripples."
    ),
    NATURE_WEATHER(
        "nature_weather",
        "Dynamic Meadow & Wildlife",
        "Serene landscape with floating clouds, soaring birds, and grazing deer."
    ),
    NEON_NEBULA(
        "neon_nebula",
        "Cosmic Deep Flow",
        "Glowing ambient floating stardust with reactive touch shockwaves."
    );

    companion object {
        fun fromId(id: String): WallpaperTheme =
            values().firstOrNull { it.id == id } ?: KOI_POND
    }
}

class WallpaperPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)

    fun getSelectedTheme(): WallpaperTheme {
        val id = prefs.getString("selected_theme", WallpaperTheme.KOI_POND.id) ?: WallpaperTheme.KOI_POND.id
        return WallpaperTheme.fromId(id)
    }

    fun setSelectedTheme(theme: WallpaperTheme) {
        prefs.edit().putString("selected_theme", theme.id).apply()
    }
}
