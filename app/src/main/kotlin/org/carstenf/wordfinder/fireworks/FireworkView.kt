package org.carstenf.wordfinder.fireworks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log // <-- Add Import
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils // Use ColorUtils as fallback/check
import org.carstenf.wordfinder.WordFinder
// import androidx.core.graphics.withAlpha // Or this if it worked
import kotlin.random.Random

class FireworkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ... (particles, paint, random, fireworkColors remain the same) ...
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var lastUpdateTime = System.currentTimeMillis()
    private val random = Random.Default
    private val fireworkColors = listOf(
        Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLUE
        // You can add more colors using Color constants or ARGB hex values like 0xFFFFD700 (Gold)
    )


    // Add onSizeChanged to see view dimensions
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        Log.d(DEBUG_TAG, "onSizeChanged: Width=$w, Height=$h")
    }

    fun startAnimation() {
        if (animator?.isRunning == true) {
            Log.d(WordFinder.TAG, "startAnimation: Animator already running.")
            return
        }
//        Log.d(DEBUG_TAG, "startAnimation: Starting animator. IsAttached: $isAttachedToWindow")

        // Add post block to ensure layout is complete
        post {
            if (!isAttachedToWindow) {
                Log.w(WordFinder.TAG, "startAnimation (post): View detached before execution.")
                return@post
            }
            if (width == 0 || height == 0) {
                Log.e(WordFinder.TAG, "startAnimation (post): View width or height is zero! Cannot launch fireworks.")
                // Consider trying again after a delay or stopping. For now, just log.
                return@post
            }

            // Log.d(DEBUG_TAG, "startAnimation (post): View Width=$width, Height=$height. Setting up animator.")
            lastUpdateTime = System.currentTimeMillis()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = Long.MAX_VALUE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    // Log sparingly here, maybe every 100 frames?
                    // if (it.currentPlayTime % 1600 < 16) { // Log roughly once per second
                    //    Log.d(DEBUG_TAG, "Animator Update: Time=${it.currentPlayTime}, Particles=${particles.size}")
                    // }
                    val currentTime = System.currentTimeMillis()
                    val deltaTime = (currentTime - lastUpdateTime).coerceAtLeast(1) / 16f // Avoid zero delta, clamp minimum time
                    lastUpdateTime = currentTime

                    updateParticles(deltaTime)
                    invalidate() // Request redraw
                }
                start()
            }
            //Log.d(DEBUG_TAG, "startAnimation (post): Animator started.")
            // Launch initial fireworks *after* animator is set up and size known
            repeat(15) { launchFirework() }
        }
    }

    fun stopAnimation() {
        //Log.d(DEBUG_TAG, "stopAnimation called.")
        animator?.cancel()
        animator = null
        particles.clear()
        invalidate()
    }

    private fun launchFirework() {
        if (width == 0 || height == 0) {
            // Log.w(DEBUG_TAG, "launchFirework: Cannot launch, view size is zero.")
            return
        }
        val startX = random.nextFloat() * width
        val startY = height.toFloat() // Start at bottom
        val targetY = random.nextFloat() * (height * 0.6f) + (height * 0.1f) // Explode in upper ~60%
        val color = fireworkColors.random(random)
        val rocket = Particle.createRocket(startX, startY, targetY, color)
        particles.add(rocket)
        // Log.d(DEBUG_TAG, "Launched Firework: count=${particles.size}, startX=$startX, targetY=$targetY, color=$color")
    }

    private fun explode(particle: Particle) {
        val explosionSize = random.nextInt(50, 120)
        val baseColor = particle.color
        val varyColor = random.nextBoolean()
//        Log.d(DEBUG_TAG, "Exploding particle at (${particle.x}, ${particle.y}) into $explosionSize particles.")

        repeat(explosionSize) {
            // ... (create explosion particle logic) ...
            val particleColor = if (varyColor && random.nextFloat() < 0.2f) Color.WHITE else baseColor
            particles.add(
                Particle.createExplosionParticle(particle.x, particle.y, particleColor)
            )
        }
        // Add log inside postDelayed
        postDelayed({
            if (animator?.isRunning == true && isAttachedToWindow) {
                // Log.d(DEBUG_TAG, "Launching next firework after explosion delay.")
                launchFirework()
            } else {
                Log.d(WordFinder.TAG, "Skipping next firework launch (animator stopped or view detached).")
            }
        }, random.nextLong(100, 800))
    }


    private fun updateParticles(deltaTime: Float) {
        if (particles.isEmpty()) return // Nothing to update

        val iterator = particles.iterator()
        val particlesToExplode = mutableListOf<Particle>()
        var removedCount = 0

        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(deltaTime) // Update position, alpha, etc.

            if (!particle.isAlive) {
                // Log particle death details (optional, can be verbose)
                // Log.d(DEBUG_TAG, "Particle died: lifetime=${particle.lifetime}, elapsed=${System.currentTimeMillis() - particle.creationTime}, alpha=${particle.alpha}")
                if (particle.isExploding && particle.targetY != null) {
                    // Check if it died AT or ABOVE its target Y (meaning it should explode)
                    if (particle.y <= particle.targetY!! || particle.alpha <= 0) { // Explode if target reached OR lifetime ended prematurely
                        // Log.d(DEBUG_TAG, "Rocket marked for explosion: y=${particle.y}, targetY=${particle.targetY}, alpha=${particle.alpha}")
                        particlesToExplode.add(particle)
                    } else {
                        Log.d(WordFinder.TAG, "Rocket died before reaching target: y=${particle.y}, targetY=${particle.targetY}")
                    }
                }
                iterator.remove() // Remove dead particle
                removedCount++
            }
        }

//        if (removedCount > 0) {
//             Log.d(WordFinder.TAG, "updateParticles: Removed $removedCount dead particles.")
//        }

        // Handle explosions separately
        particlesToExplode.forEach { explode(it) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Log infrequently to avoid spamming logcat
        // if (random.nextInt(100) == 0) { // Log roughly 1% of the time
        //    Log.d(DEBUG_TAG, "onDraw: Drawing ${particles.size} particles. Width=$width, Height=$height")
        // }

        // Simplified check: Draw background if particles exist
        if (particles.isNotEmpty()) {
            // Optional: Draw a faint background color to confirm onDraw is working and view bounds
            // canvas.drawColor(Color.argb(20, 255, 0, 0)) // Faint red background
        } else {
            // Log if onDraw is called but no particles to draw
            // Log.d(DEBUG_TAG, "onDraw: Called but no particles to draw.")
            return // Nothing to draw
        }


        var drawnCount = 0
        particles.forEach { particle ->
            // Check isAlive AND alpha > 0 explicitly
            if (particle.isAlive && particle.alpha > 0) {
                // Use ColorUtils for safety if 'withAlpha' was problematic
                paint.color = ColorUtils.setAlphaComponent(particle.color, particle.alpha)
                // paint.color = particle.color.withAlpha(particle.alpha) // Or use this if it resolved

                paint.style = Paint.Style.FILL
                canvas.drawCircle(particle.x, particle.y, particle.radius, paint)
                drawnCount++

                // Log details of the first particle being drawn (optional, very verbose)
                // if (drawnCount == 1) {
                //    Log.d(DEBUG_TAG, "Drawing particle 1: x=${particle.x}, y=${particle.y}, r=${particle.radius}, a=${particle.alpha}, color=${String.format("#%08X", paint.color)}")
                // }
            }
        }
        // Log if onDraw finished but drew nothing visible
        // if (particles.isNotEmpty() && drawnCount == 0) {
        //   Log.w(DEBUG_TAG, "onDraw: Particles exist (${particles.size}) but none were drawn (check isAlive/alpha).")
        // }
    }

    // ... (onDetachedFromWindow, onAttachedToWindow) ...
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Log.d(DEBUG_TAG, "onDetachedFromWindow: Stopping animation.")
        stopAnimation()
    }

    override fun onAttachedToWindow() {
        // Log.d(DEBUG_TAG, "onAttachedToWindow.")
        super.onAttachedToWindow()
        // Animation should be started via startAnimation() call from DialogFragment now
    }
}

// Also add logs in Particle.kt if needed, especially in update() and isAlive getter