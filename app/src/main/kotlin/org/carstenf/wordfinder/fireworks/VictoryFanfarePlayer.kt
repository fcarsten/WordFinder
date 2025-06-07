package org.carstenf.wordfinder.fireworks
import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import org.carstenf.wordfinder.R

class VictoryFanfarePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var stopHandler: Handler? = null
    private var stopRunnable: Runnable? = null

    fun playFanfare(onCompletion: (() -> Unit)? = null) {
        // Release any previous instance
        release()

        // Create new MediaPlayer
        mediaPlayer = MediaPlayer.create(context, R.raw.victory_fanfare).apply {
            setOnCompletionListener {
                cleanup()
                onCompletion?.invoke()
            }

            setOnErrorListener { _, what, extra ->
                cleanup()
                false
            }

            start()
        }

        // Setup auto-stop after 10 seconds
        stopHandler = Handler(Looper.getMainLooper())
        stopRunnable = Runnable {
            release()
            onCompletion?.invoke()
        }
        stopHandler?.postDelayed(stopRunnable!!, 10000)
    }

    fun stopFanfare() {
        release()
    }

    private fun release() {
        stopHandler?.removeCallbacks(stopRunnable ?: return)
        stopHandler = null
        stopRunnable = null

        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun cleanup() {
        stopHandler?.removeCallbacks(stopRunnable ?: return)
        stopHandler = null
        stopRunnable = null
    }
}