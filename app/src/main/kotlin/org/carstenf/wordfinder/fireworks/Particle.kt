package org.carstenf.wordfinder.fireworks

import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float, // Velocity X
    var vy: Float, // Velocity Y
    var color: Int,
    var radius: Float,
    var alpha: Int = 255,
    var lifetime: Long, // How long this particle should live (in ms)
    val creationTime: Long = System.currentTimeMillis(),
    var isExploding: Boolean = false, // For main rockets that explode
    var targetY: Float? = null // For main rockets to know when to explode
) {
    val isAlive: Boolean
        get() = (System.currentTimeMillis() - creationTime) < lifetime && alpha > 0

    fun update(deltaTime: Float, gravity: Float = 0.1f) {
        x += vx * deltaTime
        y += vy * deltaTime
        vy += gravity * deltaTime // Apply gravity

        // Fade out based on lifetime
        val elapsed = System.currentTimeMillis() - creationTime
        val lifeRatio = elapsed.toFloat() / lifetime.toFloat()
        alpha = (255 * (1f - lifeRatio)).coerceIn(0f, 255f).toInt()

        // Check if rocket should explode
        if (isExploding && targetY != null && y <= targetY!!) {
            // Mark for explosion handling in the view
            alpha = 0 // Mark as dead so it gets removed after triggering explosion
        }
    }

    companion object {
        private val random = Random.Default

        // Factory for a rising rocket
        fun createRocket(startX: Float, startY: Float, targetY: Float, color: Int): Particle {
            val speed = -(random.nextFloat() * 5 + 15) // Upward speed
            return Particle(
                x = startX,
                y = startY,
                vx = random.nextFloat() * 4 - 2, // Slight horizontal drift
                vy = speed,
                color = color,
                radius = 8f,
                lifetime = 3000L, // Rockets live longer until they explode
                isExploding = true,
                targetY = targetY
            )
        }

        // Factory for an explosion particle
        fun createExplosionParticle(startX: Float, startY: Float, color: Int): Particle {
            val angle = random.nextDouble() * 2 * Math.PI
            val speed = random.nextFloat() * 8 + 2 // Explosion speed
            return Particle(
                x = startX,
                y = startY,
                vx = (kotlin.math.cos(angle) * speed).toFloat(),
                vy = (kotlin.math.sin(angle) * speed).toFloat(),
                color = color,
                radius = random.nextFloat() * 6 + 3, // Smaller particles
                lifetime = random.nextLong(500, 1500) // Shorter lifespan
            )
        }
    }
}