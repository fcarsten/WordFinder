/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Arrays
import java.util.Locale

class GameState : ViewModel() {
	lateinit var dictionary: Dictionary

    private var autoAddPrefixalWords = false

    private val board = CharArray(16)

	val computerResultList: MutableLiveData<ArrayList<Result>?> = MutableLiveData(ArrayList())
	val playerResultList: ArrayList<Result> = ArrayList()

	val countDownTimerCurrentValue: MutableLiveData<Long> = MutableLiveData(-1L)

    fun getBoard(move: Int): Char {
        return board[move]
    }

    private val playerTaken = BooleanArray(16)

    private var solver: SolveTask? = null

    fun shuffle() {
        clearGuess()
        gameLifecycleState.postValue(GameLifeCycleState.STARTED)
        for (i in 0..15) {
            board[i] = pickRandomLetter()
        }

        computerResultList.value?.clear()
        computerResultList.postValue(computerResultList.value)
    }

    private fun pickRandomLetter(): Char {
        val r = Math.random()
        var i = 0

        var letterFreqProb = letterFreqProbEnglish

        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)) {
            letterFreqProb = letterFreqProbGerman
        }

        while (letterFreqProb[i] < r) i++

        return ('A'.code + i).toChar()
    }

    fun findWord(w: String): Boolean {
        var word = w
        word = word.uppercase(Locale.getDefault()).replace("QU".toRegex(), "Q")
        val taken = BooleanArray(16)

        val chars = word.toCharArray()
        val index = 0

        for (i in 0..15) if (findWord(chars, index, taken, i)) return true
        return false
    }

    private fun findWord(chars: CharArray, index: Int, taken: BooleanArray, move: Int): Boolean {
        if (taken[move]) return false
        if (chars[index] != board[move]) return false

        if (index == chars.size - 1) return true

        taken[move] = true

        for (i in WordFinder.MOVES[move].indices) {
            if (findWord(chars, index + 1, taken, WordFinder.MOVES[move][i])) return true
        }

        taken[move] = false

        return false
    }

    fun addComputerResult(result: Result) {
        val list = computerResultList.value
        if (list != null) {
            list.add(result)
            list.sortWith(Comparator { object1: Result, object2: Result ->
                val s1 = object1.toString().length
                val s2 = object2.toString().length
                var res = -s1.toDouble().compareTo(s2.toDouble())
                if (res == 0) res = object1.toString().compareTo(object2.toString())
                res
            })

            computerResultList.postValue(list)
        }
    }

    fun stopSolving() {
        solver?.cancel()
        solver = null
    }

    fun startSolving() {
        solver = SolveTask(this)
        solver?.execute()
    }

    fun isAvailable(i: Int): Boolean {
        return !playerTaken[i]
    }

    fun clearGuess() {
        currentGuess = ""
        lastMove = -1
        Arrays.fill(playerTaken, false)
    }

    var currentGuess: String = ""
        private set

    var lastMove: Int = -1
        private set

    var isAllow3LetterWords: Boolean = true
        set(flag) {
            field = flag
            if (!isAllow3LetterWords) {
                val iter =
                    playerResultList.iterator()
                while (iter
                        .hasNext()
                ) {
                    val next = iter.next()
                    if (next.toString().length < 4) iter.remove()
                }

                val crl =
                    computerResultList.value
                if (crl != null) {
                    val iter2 =
                        crl.iterator()
                    while (iter2
                            .hasNext()
                    ) {
                        val next = iter2.next()
                        if (next.toString().length < 4) iter2.remove()
                    }
                    computerResultList.postValue(crl)
                }
            }
        }

    private var scoreAlg = ScoreAlgorithm.COUNT

	var dictionaryName: String? = null

    fun play(move: Int) {
        lastMove = move
        currentGuess += board[move]
        playerTaken[move] = true
    }

    fun autoAddPrefixalWords(): Boolean {
        return autoAddPrefixalWords
    }

    fun setAutoAddPrefixalWords(autoAddPrefixPref: Boolean) {
        autoAddPrefixalWords = autoAddPrefixPref
    }

    enum class PlayerGuessState {
        TOO_SHORT,
        ALREADY_FOUND,
        NOT_IN_DICTIONARY
    }

    val gameLifecycleState: MutableLiveData<GameLifeCycleState> = MutableLiveData(GameLifeCycleState.NOT_STARTED)

    enum class GameLifeCycleState {
        NOT_STARTED,
        STARTED,
        TIMER_STARTED,
        TIMER_FINISHED,
        GAME_OVER
    }

    suspend fun validatePlayerGuess(guess: String): PlayerGuessState? {
        val minLength = if (isAllow3LetterWords) 3 else 4

        if (guess.length < minLength) return PlayerGuessState.TOO_SHORT

        for (result in playerResultList) {
            if (result.toString()
                    .equals(guess, ignoreCase = true)
            ) return PlayerGuessState.ALREADY_FOUND
        }
        if (dictionary.lookup(guess, dictionaryName) == null) {
            return PlayerGuessState.NOT_IN_DICTIONARY
        }

        return null
    }

    fun setScoringAlgorithm(string: String?) {
        if ("count".equals(string, ignoreCase = true)) {
            this.scoreAlg = ScoreAlgorithm.COUNT
        } else {
            this.scoreAlg = ScoreAlgorithm.VALUE
        }
    }

    private var countDownTimer: CountDownTimer? = null

	var countDownTime: Long = -1

    fun startCountDown() {
        startCountDown(countDownTime)
    }

    private fun startCountDown(time: Long) {
        countDownTimer?.cancel()

        if (countDownTime < 0) return

        gameLifecycleState.postValue(GameLifeCycleState.TIMER_STARTED)
        countDownTimerCurrentValue.postValue(time)

        countDownTimer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countDownTimerCurrentValue.postValue(millisUntilFinished / 1000)
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun getScore(list: List<Result>?): Int {
        if (list == null) return 0

        var res = 0
        when (this.scoreAlg) {
            ScoreAlgorithm.COUNT -> res = list.size
            ScoreAlgorithm.VALUE -> for (result in list) {
                res += when (result.toString().length) {
                    3, 4 -> 1
                    5 -> 2
                    6 -> 3
                    7 -> 5
                    else -> 11
                }
            }
        }
        return res
    }

    fun onPause() {
        // If game is interrupted while in progress, cancel timer and restart later in on Resume()
        if(gameLifecycleState.value == GameLifeCycleState.TIMER_STARTED ){
            countDownTimer?.cancel()
        }
    }

    fun onResume() {
        val state = gameLifecycleState.value ?: return

        // If game was interrupted while in progress continue with the left over time.
        if ( state == GameLifeCycleState.TIMER_STARTED) {
            countDownTimerCurrentValue.value?.let { startCountDown(it*1000L) }
        } else if (state >= GameLifeCycleState.TIMER_FINISHED) {
            gameLifecycleState.postValue(state) // Give UI a chance to update as well.
        }
    }

    fun cancelCountDown() {
        countDownTimer?.cancel()
    }

    val playerScore: Int
        get() = getScore(playerResultList)

    internal enum class ScoreAlgorithm {
        COUNT, VALUE
    }

    val computerScore: Int
        get() = getScore(computerResultList.value)

    companion object {
        //
        // English letter frequencies: http://en.wikipedia.org/wiki/Letter_frequency
        //
        // a 8.167%
        // b 1.492%
        // c 2.782%
        // d 4.253%
        // e 12.702%
        // f 2.228%
        // g 2.015%
        // h 6.094%
        // i 6.966%
        // j 0.153%
        // k 0.772%
        // l 4.025%
        // m 2.406%
        // n 6.749%
        // o 7.507%
        // p 1.929%
        // q 0.095%
        // r 5.987%
        // s 6.327%
        // t 9.056%
        // u 2.758%
        // v 0.978%
        // w 2.360%
        // x 0.150%
        // y 1.974%
        // z 0.074%
        // Source: https://en.wikipedia.org/wiki/Letter_frequency
        private val letterFreqProbEnglish = doubleArrayOf(
            0.07, // a 8
            0.09, // b 2
            0.12, // c 3
            0.16, // d 4
            0.27, // e 11
            0.29, // f 2
            0.32, // g 3
            0.34, // h 2
            0.43, // i 9
            0.44, // j 1
            0.45, // k 1
            0.49, // l 4
            0.52, // m 3
            0.59, // n 6
            0.66, // o 7
            0.69, // p 3
            0.70, // q 1
            0.76, // r 6
            0.81, // s 5
            0.88, // t 7
            0.92, // u 4
            0.94, // v 2
            0.96, // w 2
            0.97, // x 1
            0.99, // y 2
            1.0   // z 1
        )

        private val letterFreqProbGerman = doubleArrayOf(
            0.06, // a = 6
            0.08, // b = 2
            0.11, // c = 3
            0.16, // d = 5
            0.31, // e = 15
            0.33, // f = 2
            0.36, // g = 3
            0.41, // h = 5
            0.47, // i = 6
            0.48, // j = 1
            0.50, // k = 2
            0.53, // l = 3
            0.57, // m = 4
            0.66, // n = 9
            0.68, // o = 2
            0.69, // p = 1
            0.70, // q = 1
            0.76, // r = 6
            0.83, // s = 7
            0.89, // t = 6
            0.95, // u = 6
            0.96, // v = 1
            0.97, // w = 1
            0.98, // x = 1
            0.99, // y = 1
            1.0   // z = 1
        )
    }
}
