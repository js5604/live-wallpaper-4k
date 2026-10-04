package com.wallpaper.live

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallpaper.live.service.MultiThemeWallpaperService
import com.wallpaper.live.themes.WallpaperPreferences
import com.wallpaper.live.themes.WallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val prefs = remember { WallpaperPreferences(context) }
            var activeTheme by remember { mutableStateOf(prefs.getSelectedTheme()) }

            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)).padding(16.dp)
            ) {
                Text("Ultra 4K Live Wallpapers", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(WallpaperTheme.values()) { theme ->
                        val isSel = theme == activeTheme
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(if (isSel) 2.dp else 0.dp, if (isSel) Color(0xFF06B6D4) else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable {
                                    activeTheme = theme
                                    prefs.setSelectedTheme(theme)
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(theme.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(theme.description, color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, MultiThemeWallpaperService::class.java))
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Apply Live Wallpaper", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
