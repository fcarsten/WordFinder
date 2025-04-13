package org.carstenf.wordfinder.fireworks

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import org.carstenf.wordfinder.WordFinder
import org.carstenf.wordfinder.showGameWonDialog
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- FireworkManager.kt ---

class FireworkManager(private val wordfinder: WordFinder, private val parent: ViewGroup, private val overlay: View) {

    fun launch(config: FireworkConfig) {
        val firework = FireworkView(wordfinder, config)
        parent.addView(firework)

        config.soundResId?.let {
            MediaPlayer.create(wordfinder, it)?.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            firework.clearAnimation()
            parent.removeView(firework)
            overlay.visibility = View.GONE
            showGameWonDialog(wordfinder)
        }, config.lifetimeMs)
    }
}

// --- FireworkConfig.kt ---

data class FireworkConfig(
    val x: Float,
    val y: Float,
    val particleCount: Int = 100,
    val style: ExplosionStyle = ExplosionStyle.STARBURST,
    val color: Int = Color.WHITE,
    val hasTrails: Boolean = true,
    val hasGlow: Boolean = true,
    val soundResId: Int? = null,
    val lifetimeMs: Long = 2000L
)

enum class ExplosionStyle {
    STARBURST, RING, FOUNTAIN, RANDOM
}

// --- FireworkView.kt ---

class FireworkView(context: Context, private val config: FireworkConfig) : View(context) {

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random(System.currentTimeMillis())

    private val trailPath = Path()

    init {
        generateParticles()
        startAnimation()
    }

    private fun generateParticles() {
        val angleStep = 2 * Math.PI / config.particleCount

        for (i in 0 until config.particleCount) {
            val angle = when (config.style) {
                ExplosionStyle.STARBURST, ExplosionStyle.RING -> i * angleStep
                ExplosionStyle.FOUNTAIN -> random.nextDouble(Math.PI / 4, 3 * Math.PI / 4)
                ExplosionStyle.RANDOM -> random.nextDouble(0.0, 2 * Math.PI)
            }

            val speed = when (config.style) {
                ExplosionStyle.RING -> 6f
                else -> random.nextFloat() * 5f + 2f
            }

            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()

            particles.add(
                Particle(
                    x = config.x,
                    y = config.y,
                    velocityX = vx,
                    velocityY = vy,
                    color = config.color,
                    hasTrails = config.hasTrails,
                    hasGlow = config.hasGlow
                )
            )
        }
    }

    private fun startAnimation() {
        val frameRate = 16L
        postDelayed(object : Runnable {
            override fun run() {
                invalidate()
                postDelayed(this, frameRate)
            }
        }, frameRate)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        particles.forEach {
            it.update()
            it.draw(canvas, paint)
        }
    }
}

// --- Particle.kt ---

data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    val color: Int,
    val hasTrails: Boolean = true,
    val hasGlow: Boolean = true
) {
    private var life = 1f
    private val gravity = 0.05f
    private var previousPositions = mutableListOf<Pair<Float, Float>>()

    fun update() {
        if (hasTrails) {
            previousPositions.add(x to y)
            if (previousPositions.size > 10) previousPositions.removeAt(0)
        }

        x += velocityX
        y += velocityY
        velocityY += gravity
        life -= 0.02f
    }

    fun draw(canvas: Canvas, paint: Paint) {
        val alpha = (255 * life.coerceAtLeast(0f)).toInt()
        val radius = 5f

        if (hasGlow) {
            paint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        } else {
            paint.maskFilter = null
        }

        paint.color = color
        paint.alpha = alpha

        canvas.drawCircle(x, y, radius, paint)

        if (hasTrails && previousPositions.size >= 2) {
            paint.maskFilter = null
            paint.strokeWidth = 2f
            paint.style = Paint.Style.STROKE
            paint.alpha = alpha / 2
            val path = Path().apply {
                val (startX, startY) = previousPositions.first()
                moveTo(startX, startY)
                previousPositions.drop(1).forEach { (px, py) -> lineTo(px, py) }
            }
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
    }
}
