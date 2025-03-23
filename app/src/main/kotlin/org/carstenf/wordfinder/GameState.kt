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

        val letterCounts = IntArray(26)
        for (i in 0..15) {
            board[i] = pickRandomLetter(letterCounts, dictionaryCountryCode())
        }
        board.shuffle()

        computerResultList.value?.clear()
        computerResultList.postValue(computerResultList.value)
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
        NOT_IN_DICTIONARY,
        GUESS_VALID
    }

    val gameLifecycleState: MutableLiveData<GameLifeCycleState> =
        MutableLiveData(GameLifeCycleState.NOT_STARTED)

    enum class GameLifeCycleState {
        NOT_STARTED,
        STARTED,
        TIMER_STARTED,
        UNSOLVABLE,
        TIMER_FINISHED,
        GAME_OVER
    }

    data class PlayerGuessResult(val guess: Dictionary.WordInfoData, val state: PlayerGuessState)

    suspend fun validatePlayerGuess(guess: String): PlayerGuessResult {
        val minLength = if (isAllow3LetterWords) 3 else 4

        if (guess.length < minLength) return PlayerGuessResult(
            Dictionary.WordInfoData(guess),
            PlayerGuessState.TOO_SHORT
        )

        val lookupResult = dictionary.lookup(guess, dictionaryName)
            ?: return PlayerGuessResult(
                Dictionary.WordInfoData(guess),
                PlayerGuessState.NOT_IN_DICTIONARY
            )

        for (result in playerResultList) {
            if (result.toString()
                    .equals(lookupResult.displayText, ignoreCase = true)
            ) return PlayerGuessResult(
                Dictionary.WordInfoData(guess),
                PlayerGuessState.ALREADY_FOUND
            )
        }

        return PlayerGuessResult(lookupResult, PlayerGuessState.GUESS_VALID)
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
        countDownTimerCurrentValue.postValue(time / 1000)

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
        if (gameLifecycleState.value == GameLifeCycleState.TIMER_STARTED) {
            countDownTimer?.cancel()
        }
    }

    fun onResume() {
        val state = gameLifecycleState.value ?: return

        // If game was interrupted while in progress continue with the left over time.
        if (state == GameLifeCycleState.TIMER_STARTED) {
            countDownTimerCurrentValue.value?.let { startCountDown(it * 1000L) }
        } else if (state >= GameLifeCycleState.TIMER_FINISHED) {
            gameLifecycleState.postValue(state) // Give UI a chance to update as well.
        }
    }

    fun cancelCountDown() {
        countDownTimer?.cancel()
    }

    fun onSolveFinished() {
        if (computerResultList.value?.size == 0) {
            countDownTimer?.cancel()
            gameLifecycleState.postValue(GameLifeCycleState.UNSOLVABLE)
        }
    }

    fun dictionaryCountryCode(): LANGUAGE {
        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_wiki", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)) {
            return LANGUAGE.DE
        }
        return LANGUAGE.EN
    }

    val playerScore: Int
        get() = getScore(playerResultList)

    internal enum class ScoreAlgorithm {
        COUNT, VALUE
    }

    val computerScore: Int
        get() = getScore(computerResultList.value)

}