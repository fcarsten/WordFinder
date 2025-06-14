package org.carstenf.wordfinder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CountUpTimer(private val time: Long= 0, private val onTick: (Long) -> Unit) {
    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.Main).launch {
            var seconds = time
            while (true) {
                onTick(seconds++)
                delay(1000L)
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }
}