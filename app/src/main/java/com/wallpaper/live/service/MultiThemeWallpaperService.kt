package com.wallpaper.live.service

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.wallpaper.live.themes.WallpaperPreferences
import com.wallpaper.live.themes.WallpaperTheme
import kotlin.math.*
import kotlin.random.Random

// Move data models to top-level to satisfy Kotlin compiler
data class WaterRipple(var x: Float, var y: Float, var radius: Float = 0f, var alpha: Int = 220)
data class KoiFish(var x: Float, var y: Float, var speed: Float, var angle: Float, val size: Float, val bodyColor: Int, val finColor: Int, var wiggle: Float = 0f)
data class LandscapeCloud(var x: Float, var y: Float, val speed: Float, val scale: Float)
data class LandscapeBird(var x: Float, var y: Float, var vx: Float, var vy: Float, var wing: Float = 0f)
data class LandscapeDeer(var x: Float, var y: Float, var isEating: Boolean = true, var timer: Int = 100)

class MultiThemeWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = WallpaperEngine()

    inner class WallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private lateinit var prefs: WallpaperPreferences

        private val ripples = mutableListOf<WaterRipple>()
        private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        private val fishes = mutableListOf<KoiFish>()
        private val fishPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val clouds = mutableListOf<LandscapeCloud>()
        private val birds = mutableListOf<LandscapeBird>()
        private val deers = mutableListOf<LandscapeDeer>()
        private val landPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val loop = object : Runnable {
            override fun run() {
                draw()
                if (isVisible) handler.postDelayed(this, 16)
            }
        }

        override fun onCreate(holder: SurfaceHolder?) {
            super.onCreate(holder)
            prefs = WallpaperPreferences(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) handler.post(loop) else handler.removeCallbacks(loop)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, format, w, h)
            val fw = w.toFloat()
            val fh = h.toFloat()

            fishes.clear()
            val colors = listOf(
                Pair(Color.parseColor("#FF5722"), Color.parseColor("#FFCCBC")),
                Pair(Color.WHITE, Color.parseColor("#D32F2F")),
                Pair(Color.parseColor("#FFB300"), Color.parseColor("#FFE082"))
            )
            for (i in 0..6) {
                val p = colors[i % colors.size]
                fishes.add(KoiFish(Random.nextFloat() * fw, Random.nextFloat() * fh, Random.nextFloat() * 1.5f + 1.2f, Random.nextFloat() * 6.28f, Random.nextFloat() * 15f + 35f, p.first, p.second))
            }
            clouds.clear()
            for (i in 0..3) clouds.add(LandscapeCloud(Random.nextFloat() * fw, Random.nextFloat() * (fh * 0.3f), Random.nextFloat() * 0.4f + 0.2f, Random.nextFloat() * 0.5f + 0.8f))
            birds.clear()
            for (i in 0..4) birds.add(LandscapeBird(Random.nextFloat() * fw, Random.nextFloat() * (fh * 0.4f), Random.nextFloat() * 2f + 1.5f, 0f))
            deers.clear()
            deers.add(LandscapeDeer(fw * 0.25f, fh * 0.78f))
            deers.add(LandscapeDeer(fw * 0.70f, fh * 0.82f))
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val tx = event.x
                val ty = event.y
                if (ripples.size < 10) ripples.add(WaterRipple(tx, ty))
                for (f in fishes) {
                    val dx = f.x - tx
                    val dy = f.y - ty
                    if (sqrt(dx * dx + dy * dy) < 350f) {
                        f.angle = atan2(dy, dx)
                        f.speed = 7f
                    }
                }
                for (b in birds) {
                    val dx = b.x - tx
                    val dy = b.y - ty
                    if (sqrt(dx * dx + dy * dy) < 300f) { b.vy = -3f; b.vx = 4f }
                }
            }
            super.onTouchEvent(event)
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    when (prefs.getSelectedTheme()) {
                        WallpaperTheme.KOI_POND -> {
                            fishPaint.shader = LinearGradient(0f, 0f, w, h, Color.parseColor("#00293C"), Color.parseColor("#00151F"), Shader.TileMode.CLAMP)
                            canvas.drawRect(0f, 0f, w, h, fishPaint)
                            fishPaint.shader = null
                            fishPaint.color = Color.parseColor("#145A32")
                            canvas.drawCircle(w * 0.15f, h * 0.25f, 80f, fishPaint)
                            canvas.drawCircle(w * 0.85f, h * 0.70f, 100f, fishPaint)
                            for (f in fishes) {
                                if (f.speed > 1.8f) f.speed *= 0.96f
                                f.angle += (Random.nextFloat() - 0.5f) * 0.08f
                                f.x += cos(f.angle) * f.speed
                                f.y += sin(f.angle) * f.speed
                                f.wiggle += 0.2f
                                if (f.x < -80f) f.x = w + 60f
                                if (f.x > w + 80f) f.x = -60f
                                if (f.y < -80f) f.y = h + 60f
                                if (f.y > h + 80f) f.y = -60f

                                canvas.save()
                                canvas.translate(f.x, f.y)
                                canvas.rotate((f.angle * 180f / Math.PI.toFloat()) + 90f)
                                val wOff = sin(f.wiggle) * (f.size * 0.2f)
                                fishPaint.color = f.bodyColor
                                val path = Path().apply {
                                    moveTo(0f, -f.size)
                                    quadTo(f.size * 0.4f, 0f, wOff, f.size)
                                    quadTo(-f.size * 0.4f, 0f, 0f, -f.size)
                                }
                                canvas.drawPath(path, fishPaint)
                                canvas.restore()
                            }
                        }
                        WallpaperTheme.NATURE_WEATHER -> {
                            landPaint.shader = LinearGradient(0f, 0f, 0f, h * 0.7f, Color.parseColor("#4A709C"), Color.parseColor("#E0A96D"), Shader.TileMode.CLAMP)
                            canvas.drawRect(0f, 0f, w, h, landPaint)
                            landPaint.shader = null
                            landPaint.color = Color.parseColor("#FFF2B2")
                            canvas.drawCircle(w * 0.75f, h * 0.2f, 40f, landPaint)
                            landPaint.color = Color.WHITE
                            landPaint.alpha = 130
                            for (c in clouds) {
                                c.x += c.speed
                                if (c.x > w + 100f) c.x = -100f
                                canvas.drawCircle(c.x, c.y, 35f * c.scale, landPaint)
                                canvas.drawCircle(c.x + 30f * c.scale, c.y - 10f * c.scale, 45f * c.scale, landPaint)
                            }
                            landPaint.color = Color.parseColor("#1B4D3E")
                            landPaint.alpha = 255
                            val hill = Path().apply {
                                moveTo(0f, h * 0.75f)
                                quadTo(w * 0.5f, h * 0.68f, w, h * 0.78f)
                                lineTo(w, h); lineTo(0f, h)
                            }
                            canvas.drawPath(hill, landPaint)
                            landPaint.color = Color.parseColor("#263238")
                            landPaint.style = Paint.Style.STROKE
                            landPaint.strokeWidth = 3f
                            for (b in birds) {
                                b.x += b.vx; b.y += b.vy; b.wing += 0.2f
                                if (b.x > w + 40f) b.x = -30f
                                val wOff = sin(b.wing) * 8f
                                val bp = Path().apply {
                                    moveTo(b.x - 12f, b.y + wOff)
                                    quadTo(b.x, b.y - 4f, b.x + 12f, b.y + wOff)
                                }
                                canvas.drawPath(bp, landPaint)
                            }
                            landPaint.style = Paint.Style.FILL
                        }
                        WallpaperTheme.NEON_NEBULA -> {
                            fishPaint.color = Color.parseColor("#070B19")
                            canvas.drawRect(0f, 0f, w, h, fishPaint)
                            fishPaint.color = Color.parseColor("#00DFD8")
                            for (f in fishes) {
                                f.x += cos(f.angle) * 1.2f
                                f.y += sin(f.angle) * 1.2f
                                if (f.x < 0f) f.x = w
                                if (f.x > w) f.x = 0f
                                if (f.y < 0f) f.y = h
                                if (f.y > h) f.y = 0f
                                canvas.drawCircle(f.x, f.y, f.size * 0.25f, fishPaint)
                            }
                        }
                    }
                    ripplePaint.color = if (prefs.getSelectedTheme() == WallpaperTheme.KOI_POND) Color.parseColor("#80D8FF") else Color.parseColor("#FF4081")
                    val it = ripples.iterator()
                    while (it.hasNext()) {
                        val r = it.next()
                        r.radius += 5f
                        r.alpha -= 4
                        if (r.alpha <= 0) it.remove()
                        else {
                            ripplePaint.alpha = r.alpha
                            canvas.drawCircle(r.x, r.y, r.radius, ripplePaint)
                        }
                    }
                }
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(loop)
        }
    }
}
