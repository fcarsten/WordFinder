/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Arrays
import java.util.Locale

class GameState : ViewModel() {
    var solveFinished: Boolean = false
    private var letterSelector: LETTER_RANDOM_DIST? = LETTER_RANDOM_DIST.MULTI_LETTER_FREQUENCY

    var timerMode: TIMER_MODE = TIMER_MODE.COUNT_DOWN

    lateinit var dictionary: Dictionary

    private var autoAddPrefixalWords = false

    private val board = CharArray(16)

    val computerResultList: MutableLiveData<ArrayList<Result>?> = MutableLiveData(ArrayList())
    val playerResultList: ArrayList<Result> = ArrayList()

    val timerCurrentValue: MutableLiveData<Long> = MutableLiveData(0L)

    fun getBoard(move: Int): Char {
        return board[move]
    }

    private val playerTaken = BooleanArray(16)

    private var solver: SolveTask? = null

    fun shuffle() {
        clearGuess()
        gameLifecycleState.postValue(GameLifeCycleState.STARTED)

        val letterCounts = IntArray(26)

        var letterRandomDistCur = letterSelector
        if(letterRandomDistCur == null) {
            Log.d(WordFinder.TAG, "Letter selector: Whatever")
            letterRandomDistCur =  LETTER_RANDOM_DIST.entries.toTypedArray().random()
        }

        Log.d(WordFinder.TAG, "Letter selector: ${letterRandomDistCur.name}")

        for (i in 0..15) {
            board[i] = pickRandomLetter(letterRandomDistCur, letterCounts, dictionaryCountryCode(), i).uppercaseChar()
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
        solveFinished = true
        solver = null
    }

    fun startSolving() {
        solver = SolveTask(this)
        solveFinished = false
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

    enum class LETTER_RANDOM_DIST {
        UNIFORM,
        LETTER_FREQUENCY,
        MULTI_LETTER_FREQUENCY,
        LETTER_DICE
    }

    enum class TIMER_MODE {
        COUNT_DOWN,
        STOP_WATCH
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
    var countUpTimer: CountUpTimer? = null

    var gameTime: Long = -1

    fun startTimer() {
        startTimer(gameTime)
    }

    private fun startTimer(time: Long) {
        cancelTimer()

        if (gameTime < 0) return

        gameLifecycleState.postValue(GameLifeCycleState.TIMER_STARTED)
        timerCurrentValue.postValue(time / 1000)

        if(timerMode == TIMER_MODE.COUNT_DOWN) {
            countDownTimer = object : CountDownTimer(time, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timerCurrentValue.postValue(millisUntilFinished / 1000)
                }

                override fun onFinish() {
                }
            }.start()
        } else {
            countUpTimer =  CountUpTimer{ seconds: Long -> timerCurrentValue.postValue(seconds)}
            countUpTimer?.start()
        }

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
            countUpTimer?.cancel()
        }
    }

    fun onResume() {
        val state = gameLifecycleState.value ?: return

        // If game was interrupted while in progress continue with the left over time.
        if (state == GameLifeCycleState.TIMER_STARTED) {
            timerCurrentValue.value?.let { startTimer(it * 1000L) }
        } else if (state >= GameLifeCycleState.TIMER_FINISHED) {
            gameLifecycleState.postValue(state) // Give UI a chance to update as well.
        }
    }

    fun cancelTimer() {
        countDownTimer?.cancel()
        countUpTimer?.cancel()
    }

    fun onSolveFinished() {
        if (computerResultList.value?.size == 0) {
            cancelTimer()
            gameLifecycleState.postValue(GameLifeCycleState.UNSOLVABLE)
        }
        solveFinished = true
    }

    fun dictionaryCountryCode(): LANGUAGE {
        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_wiki", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)) {
            return LANGUAGE.DE
        }
        return LANGUAGE.EN
    }

    fun setLetterSelector(distStr: String?) {
        if ("uniformRandom".equals(distStr, ignoreCase = true)) {
            letterSelector = LETTER_RANDOM_DIST.UNIFORM
        } else if ("letterFrequency".equals(distStr, ignoreCase = true)) {
            letterSelector = LETTER_RANDOM_DIST.LETTER_FREQUENCY
        } else if ("multiLetterFrequence".equals(distStr, ignoreCase = true)) {
            letterSelector = LETTER_RANDOM_DIST.MULTI_LETTER_FREQUENCY
        } else if ("letterDice".equals(distStr, ignoreCase = true)) {
            letterSelector = LETTER_RANDOM_DIST.LETTER_DICE
        }  else if ("whateverRandom".equals(distStr, ignoreCase = true)) {
            letterSelector = null
        }
    }

    val playerScore: Int
        get() = getScore(playerResultList)

    internal enum class ScoreAlgorithm {
        COUNT, VALUE
    }

    val computerScore: Int
        get() = getScore(computerResultList.value)

}