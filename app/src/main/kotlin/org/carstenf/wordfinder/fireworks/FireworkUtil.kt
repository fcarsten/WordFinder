package org.carstenf.wordfinder.fireworks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Firework(private val x: Float, private val y: Float) {
    private val particles = List(50) { Particle(x, y) }

    fun update() {
        particles.forEach { it.update() }
    }

    fun draw(canvas: Canvas, paint: Paint) {
        particles.forEach { it.draw(canvas, paint) }
    }

    fun isFinished(): Boolean {
        return particles.all { it.alpha <= 0f }
    }
}

class Particle(private var x: Float, private var y: Float) {
    private val angle = Random.nextFloat() * 2f * Math.PI
    private val speed = Random.nextFloat() * 5f + 2f // random speed between 2 and 7

    private var velocityX = (cos(angle) * speed).toFloat()
    private var velocityY = (sin(angle) * speed).toFloat()
//    private var velocityX: Float = Random.nextFloat() * 6f - 3f
//    private var velocityY: Float = Random.nextFloat() * 6f - 3f
    private val color: Int = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
    private var life: Float = 10f
    private val gravity = 0.05f
    var alpha = 1f

    fun update() {
        x += velocityX
        y += velocityY
        velocityY += gravity
        life -= 0.02f
        alpha = life.coerceAtLeast(0f)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        paint.alpha = (255 * alpha).toInt()
        canvas.drawCircle(x, y, 5f+Random.nextFloat()*10f, paint)
    }
}
