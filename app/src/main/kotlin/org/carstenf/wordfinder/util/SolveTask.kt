/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.carstenf.wordfinder.GameState
import org.carstenf.wordfinder.WordFinder
import org.carstenf.wordfinder.dictionary.Dictionary

class SolveTask(private val gameState: GameState) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun execute() {
        scope.launch {
            val foundWords = mutableListOf<Result>()
            Thread.currentThread().priority = Thread.MIN_PRIORITY
            solveBoard(foundWords)
            gameState.onSolveFinished(foundWords)
        }
    }

    fun cancel() {
        scope.cancel()
    }

    private suspend fun solveBoard(foundWords: MutableList<Result>) {
        val prefixes = HashSet<String>()
        val taken = BooleanArray(16)
        for (i in 0 until 16) {
            findAnyWord(i, taken, 2, "", prefixes)
        }

        for (prefix in prefixes) {
            findAllWordsForPrefix(prefix, foundWords)
        }
    }

    private fun findAnyWord(move: Int, taken: BooleanArray, depth: Int, res: String, prefixes: HashSet<String>) {
        taken[move] = true
        if (depth == 0) {
            val prefix = res + gameState.getBoard(move)
            prefixes.add(prefix)
            if(prefix[0] == 'Q' && prefix[1] != 'U') {
                prefixes.add(""+prefix[0]+'U'+prefix[1])
            }

            if(prefix[1] == 'Q' && prefix[2] != 'U') {
                prefixes.add(""+prefix[0]+prefix[1]+'U')
            }
        } else {
            for (next in WordFinder.MOVES[move]) {
                if (!taken[next]) {
                    findAnyWord(next, taken, depth - 1, res + gameState.getBoard(move), prefixes)
                }
            }
        }
        taken[move] = false
    }

    private suspend fun findAllWordsForPrefix(prefix: String, foundWords: MutableList<Result>) {
        val resultList = gameState.dictionary.getAllWords(prefix, gameState.dictionaryName) ?: return
        val foundWordsForPrefix = mutableListOf<Dictionary.WordInfoData>()
        for (result in resultList) {
            val minLength = if (gameState.isAllow3LetterWords) 3 else 4
            if (result.text.length >= minLength && gameState.findWord(result.text)) {
                Log.d(WordFinder.TAG, "Found: $result") // NON-NLS
                foundWordsForPrefix.add(result)
                foundWords.add(Result(result))
            }
        }
        if (foundWordsForPrefix.isNotEmpty()) {
            publishProgress(foundWordsForPrefix)
        }
    }

    private suspend fun publishProgress(words: List<Dictionary.WordInfoData>) {
       val results = words.map { Result(it) }
       gameState.addComputerResults(results)
    }
}