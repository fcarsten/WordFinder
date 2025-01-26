package org.carstenf.wordfinder

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SolveTask(private val gameState: GameState) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun execute() {
        scope.launch {
            Thread.currentThread().priority = Thread.MIN_PRIORITY
            solve2()
        }
    }

    fun cancel() {
        scope.cancel()
    }

    private fun solve2() {
        val prefixes = HashSet<String>()
        val taken = BooleanArray(16)
        for (i in 0 until 16) {
            findAnyWord(i, taken, 2, "", prefixes)
        }

        for (prefix in prefixes) {
            solve1(prefix)
        }
    }

    private fun findAnyWord(move: Int, taken: BooleanArray, depth: Int, res: String, prefixes: HashSet<String>) {
        taken[move] = true
        if (depth == 0) {
            Log.i(WordFinder.TAG, res + gameState.getBoard(move))
            prefixes.add(res + gameState.getBoard(move))
        } else {
            for (next in WordFinder.MOVES[move]) {
                if (!taken[next]) {
                    findAnyWord(next, taken, depth - 1, res + gameState.getBoard(move), prefixes)
                }
            }
        }
        taken[move] = false
    }

    private fun solve1(prefix: String) {
        val cursor = gameState.dictionary.getAllWords(prefix, gameState.dictionaryName)
        cursor.use {
            if (it == null) return
            it.moveToFirst()
            do {
                val word = it.getString(0)
                val minLength = if (gameState.isAllow3LetterWords) 3 else 4
                if (word.length >= minLength && gameState.findWord(word)) {
                    Log.d(WordFinder.TAG, "Found: $word")
                    publishProgress(word)
                }
            } while (it.moveToNext())
        }
    }

    private fun publishProgress(word: String) {
        CoroutineScope(Dispatchers.Main).launch {
            gameState.addComputerResult(Result(word))
        }
    }
}