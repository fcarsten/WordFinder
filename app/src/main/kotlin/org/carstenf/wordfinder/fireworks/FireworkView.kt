package org.carstenf.wordfinder.fireworks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class FireworkViewOld(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val fireworks = mutableListOf<Firework>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val random = Random(System.currentTimeMillis())
    private val frameRate = 16L // ~60fps

    init {
        postDelayed({ launchFirework() }, 100)
        startAnimationLoop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val iterator = fireworks.iterator()
        while (iterator.hasNext()) {
            val fw = iterator.next()
            fw.update()
            fw.draw(canvas, paint)

            if (fw.isFinished()) {
                iterator.remove()
            }
        }
    }

    private fun launchFirework() {
        val w = width / 2f
        val h = height / 3f
        val centerX = w + random.nextInt((-w).toInt(), w.toInt())
        val centerY = h + random.nextInt((-h).toInt(), h.toInt())
        fireworks.add(Firework(centerX, centerY))
        postDelayed({ launchFirework() }, 200)
    }

    private fun startAnimationLoop() {
        postDelayed(object : Runnable {
            override fun run() {
                invalidate()
                postDelayed(this, frameRate)
            }
        }, frameRate)
    }
}

