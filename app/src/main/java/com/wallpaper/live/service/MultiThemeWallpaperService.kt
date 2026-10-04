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

class MultiThemeWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WallpaperEngine()

    inner class WallpaperEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false
        private lateinit var prefs: WallpaperPreferences

        private data class WaterRipple(var x: Float, var y: Float, var radius: Float = 0f, var alpha: Int = 220)
        private val ripples = mutableListOf<WaterRipple>()
        private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        private data class KoiFish(
            var x: Float,
            var y: Float,
            var speed: Float,
            var angle: Float,
            val size: Float,
            val bodyColor: Int,
            val finColor: Int,
            var wigglePhase: Float = 0f
        )
        private val fishes = mutableListOf<KoiFish>()
        private val fishPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private data class Cloud(var x: Float, var y: Float, val speed: Float, val scale: Float)
        private data class Bird(var x: Float, var y: Float, var vx: Float, var vy: Float, var wingPhase: Float = 0f)
        private data class Deer(var x: Float, var y: Float, var isEating: Boolean = true, var stateTimer: Int = 100)
        
        private val clouds = mutableListOf<Cloud>()
        private val birds = mutableListOf<Bird>()
        private val deers = mutableListOf<Deer>()
        private val landscapePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        private val loopRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (isVisible) {
                    handler.postDelayed(this, 16)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs = WallpaperPreferences(applicationContext)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                handler.post(loopRunnable)
            } else {
                handler.removeCallbacks(loopRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            initScene(width, height)
        }

        private fun initScene(width: Int, height: Int) {
            fishes.clear()
            val fishPalette = listOf(
                Pair(Color.parseColor("#FF5722"), Color.parseColor("#FFCCBC")),
                Pair(Color.parseColor("#FFFFFF"), Color.parseColor("#D32F2F")),
                Pair(Color.parseColor("#FFB300"), Color.parseColor("#FFE082")),
                Pair(Color.parseColor("#212121"), Color.parseColor("#FF5722"))
            )
            for (i in 0..7) {
                val p = fishPalette[i % fishPalette.size]
                fishes.add(
                    KoiFish(
                        x = Random.nextFloat() * width,
                        y = Random.nextFloat() * height,
                        speed = Random.nextFloat() * 1.5f + 1.2f,
                        angle = Random.nextFloat() * (2 * Math.PI.toFloat()),
                        size = Random.nextFloat() * 20f + 35f,
                        bodyColor = p.first,
                        finColor = p.second
                    )
                )
            }

            clouds.clear()
            for (i in 0..4) {
                clouds.add(Cloud(Random.nextFloat() * width, Random.nextFloat() * (height * 0.35f), Random.nextFloat() * 0.4f + 0.2f, Random.nextFloat() * 0.5f + 0.8f))
            }

            birds.clear()
            for (i in 0..5) {
                birds.add(Bird(Random.nextFloat() * width, Random.nextFloat() * (height * 0.4f), Random.nextFloat() * 2f + 1.5f, Random.nextFloat() * 0.4f - 0.2f))
            }

            deers.clear()
            deers.add(Deer(width * 0.25f, height * 0.78f))
            deers.add(Deer(width * 0.70f, height * 0.82f))
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                val touchX = event.x
                val touchY = event.y

                if (ripples.size < 12) {
                    ripples.add(WaterRipple(touchX, touchY))
                }

                // Fish swim away from touch
                for (f in fishes) {
                    val dx = f.x - touchX
                    val dy = f.y - touchY
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance < 380f && distance > 1f) {
                        f.angle = atan2(dy, dx)
                        f.speed = 7.5f
                    }
                }

                // Birds scatter upwards
                for (b in birds) {
                    val dx = b.x - touchX
                    val dy = b.y - touchY
                    if (sqrt(dx * dx + dy * dy) < 300f) {
                        b.vy = -3.5f
                        b.vx = 4f
                    }
                }
            }
            super.onTouchEvent(event)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    when (prefs.getSelectedTheme()) {
                        WallpaperTheme.KOI_POND -> drawKoiPond(canvas)
                        WallpaperTheme.NATURE_WEATHER -> drawNatureLandscape(canvas)
                        WallpaperTheme.NEON_NEBULA -> drawNebulaScene(canvas)
                    }
                    drawRipples(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun drawKoiPond(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            val waterGrad = LinearGradient(0f, 0f, w, h, Color.parseColor("#00293C"), Color.parseColor("#00151F"), Shader.TileMode.CLAMP)
            fishPaint.shader = waterGrad
            fishPaint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, w, h, fishPaint)
            fishPaint.shader = null

            fishPaint.color = Color.parseColor("#145A32")
            canvas.drawCircle(w * 0.15f, h * 0.25f, 90f, fishPaint)
            canvas.drawCircle(w * 0.85f, h * 0.70f, 110f, fishPaint)

            for (f in fishes) {
                if (f.speed > 1.8f) f.speed *= 0.96f
                f.angle += (Random.nextFloat() - 0.5f) * 0.08f
                f.x += cos(f.angle) * f.speed
                f.y += sin(f.angle) * f.speed
                f.wigglePhase += 0.22f

                if (f.x < -100) f.x = w + 80
                if (f.x > w + 100) f.x = -80
                if (f.y < -100) f.y = h + 80
                if (f.y > h + 100) f.y = -80

                canvas.save()
                canvas.translate(f.x, f.y)
                canvas.rotate((f.angle * 180f / Math.PI.toFloat()) + 90f)

                val wiggle = sin(f.wigglePhase) * (f.size * 0.25f)

                fishPaint.color = f.finColor
                fishPaint.alpha = 180
                canvas.drawOval(RectF(-f.size * 0.6f, -f.size * 0.2f, -f.size * 0.2f, f.size * 0.4f), fishPaint)
                canvas.drawOval(RectF(f.size * 0.2f, -f.size * 0.2f, f.size * 0.6f, f.size * 0.4f), fishPaint)

                fishPaint.color = f.bodyColor
                fishPaint.alpha = 245
                val fishPath = Path().apply {
                    moveTo(0f, -f.size * 1.1f)
                    quadTo(f.size * 0.4f, 0f, wiggle, f.size * 1.2f)
                    quadTo(-f.size * 0.4f, 0f, 0f, -f.size * 1.1f)
                }
                canvas.drawPath(fishPath, fishPaint)
                canvas.restore()
            }
        }

        private fun drawNatureLandscape(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            val sky = LinearGradient(0f, 0f, 0f, h * 0.7f, Color.parseColor("#4A709C"), Color.parseColor("#E0A96D"), Shader.TileMode.CLAMP)
            landscapePaint.shader = sky
            canvas.drawRect(0f, 0f, w, h, landscapePaint)
            landscapePaint.shader = null

            landscapePaint.color = Color.parseColor("#FFF2B2")
            landscapePaint.alpha = 220
            canvas.drawCircle(w * 0.75f, h * 0.22f, 48f, landscapePaint)

            landscapePaint.color = Color.WHITE
            landscapePaint.alpha = 130
            for (c in clouds) {
                c.x += c.speed
                if (c.x > w + 150) c.x = -150f
                canvas.drawCircle(c.x, c.y, 40f * c.scale, landscapePaint)
                canvas.drawCircle(c.x + 35f * c.scale, c.y - 12f * c.scale, 50f * c.scale, landscapePaint)
            }

            landscapePaint.color = Color.parseColor("#1B4D3E")
            landscapePaint.alpha = 255
            val hillPath = Path().apply {
                moveTo(0f, h * 0.75f)
                quadTo(w * 0.45f, h * 0.68f, w, h * 0.79f)
                lineTo(w, h)
                lineTo(0f, h)
            }
            canvas.drawPath(hillPath, landscapePaint)

            landscapePaint.color = Color.parseColor("#263238")
            landscapePaint.style = Paint.Style.STROKE
            landscapePaint.strokeWidth = 3f
            for (b in birds) {
                b.x += b.vx
                b.y += b.vy
                b.wingPhase += 0.2f
                if (b.x > w + 50) b.x = -40f
                val wingOffset = sin(b.wingPhase) * 10f
                val birdPath = Path().apply {
                    moveTo(b.x - 14f, b.y + wingOffset)
                    quadTo(b.x, b.y - 5f, b.x + 14f, b.y + wingOffset)
                }
                canvas.drawPath(birdPath, landscapePaint)
            }
            landscapePaint.style = Paint.Style.FILL

            landscapePaint.color = Color.parseColor("#14211D")
            for (d in deers) {
                d.stateTimer--
                if (d.stateTimer <= 0) {
                    d.isEating = !d.isEating
                    d.stateTimer = Random.nextInt(150, 300)
                }
                canvas.save()
                canvas.translate(d.x, d.y)
                canvas.drawRoundRect(RectF(-18f, -22f, 18f, 0f), 8f, 8f, landscapePaint)
                canvas.drawRect(-15f, 0f, -10f, 30f, landscapePaint)
                canvas.drawRect(10f, 0f, 15f, 30f, landscapePaint)
                val neckY = if (d.isEating) 6f else -24f
                canvas.drawRoundRect(RectF(12f, neckY, 26f, neckY + 16f), 4f, 4f, landscapePaint)
                canvas.restore()
            }
        }

        private fun drawNebulaScene(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            fishPaint.color = Color.parseColor("#070B19")
            canvas.drawRect(0f, 0f, w, h, fishPaint)

            fishPaint.color = Color.parseColor("#00DFD8")
            fishPaint.alpha = 180
            for (f in fishes) {
                f.x += cos(f.angle) * 1.5f
                f.y += sin(f.angle) * 1.5f
                if (f.x < 0) f.x = w
                if (f.x > w) f.x = 0f
                if (f.y < 0) f.y = h
                if (f.y > h) f.y = 0f
                canvas.drawCircle(f.x, f.y, f.size * 0.3f, fishPaint)
            }
        }

        private fun drawRipples(canvas: Canvas) {
            ripplePaint.color = if (prefs.getSelectedTheme() == WallpaperTheme.KOI_POND)
                Color.parseColor("#80D8FF")
            else
                Color.parseColor("#FF4081")

            val it = ripples.iterator()
            while (it.hasNext()) {
                val r = it.next()
                r.radius += 5.5f
                r.alpha -= 4
                if (r.alpha <= 0) {
                    it.remove()
                } else {
                    ripplePaint.alpha = r.alpha
                    canvas.drawCircle(r.x, r.y, r.radius, ripplePaint)
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(loopRunnable)
        }
    }
}
