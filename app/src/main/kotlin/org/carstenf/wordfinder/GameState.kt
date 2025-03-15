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
            board[i] = pickRandomLetter(letterCounts)
        }
        board.shuffle()

        computerResultList.value?.clear()
        computerResultList.postValue(computerResultList.value)
    }

    private fun pickRandomLetter(letterCounts: IntArray): Char {
        var letterCountMatrix = letterCountsEnglish

        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_wiki", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)
        ) {
            letterCountMatrix = letterCountsGerman
        }

        var totalCount = 0
        for (k in 0..25) {
            totalCount += letterCountMatrix[k][letterCounts[k]]
        }

        var r = Math.random() * totalCount
        var i = 0

        while (letterCountMatrix[i][letterCounts[i]] < r) {
            r -= letterCountMatrix[i][letterCounts[i]]
            i += 1
        }

        if (letterCounts[i] < 10) // Make sure we never
            letterCounts[i]++
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

    fun dictionaryCountryCode(): String {
        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_wiki", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)) {
            return "DE"
        }
        return "EN"
    }

    val playerScore: Int
        get() = getScore(playerResultList)

    internal enum class ScoreAlgorithm {
        COUNT, VALUE
    }

    val computerScore: Int
        get() = getScore(computerResultList.value)

    companion object {
        // Letter a: [33735, 6997, 547, 44, 2, 1, 0, 0, 0, 0]
        // Letter b: [9773, 815, 53, 2, 0, 0, 0, 0, 0, 0]
        // Letter c: [19491, 2717, 141, 0, 0, 0, 0, 0, 0, 0]
        // Letter d: [19196, 2487, 188, 7, 0, 0, 0, 0, 0, 0]
        // Letter e: [45137, 15464, 3071, 287, 12, 0, 0, 0, 0, 0]
        // Letter f: [6948, 877, 24, 1, 0, 0, 0, 0, 0, 0]
        // Letter g: [15542, 1645, 167, 9, 0, 0, 0, 0, 0, 0]
        // Letter h: [11844, 852, 18, 0, 0, 0, 0, 0, 0, 0]
        // Letter i: [36396, 10132, 1811, 264, 20, 1, 0, 0, 0, 0]
        // Letter j: [974, 6, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter k: [5164, 241, 8, 2, 0, 0, 0, 0, 0, 0]
        // Letter l: [24204, 4402, 393, 23, 0, 0, 0, 0, 0, 0]
        // Letter m: [13410, 1425, 60, 2, 0, 0, 0, 0, 0, 0]
        // Letter n: [30710, 6644, 768, 44, 2, 0, 0, 0, 0, 0]
        // Letter o: [27453, 6177, 623, 35, 2, 0, 0, 0, 0, 0]
        // Letter p: [14380, 1830, 98, 4, 0, 0, 0, 0, 0, 0]
        // Letter q: [1023, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter r: [33120, 6027, 457, 8, 0, 0, 0, 0, 0, 0]
        // Letter s: [37683, 10743, 1932, 298, 47, 4, 0, 0, 0, 0]
        // Letter t: [30626, 6827, 558, 18, 0, 0, 0, 0, 0, 0]
        // Letter u: [16804, 1406, 72, 7, 0, 0, 0, 0, 0, 0]
        // Letter v: [5414, 177, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter w: [5029, 183, 4, 0, 0, 0, 0, 0, 0, 0]
        // Letter x: [1544, 4, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter y: [8378, 217, 1, 0, 0, 0, 0, 0, 0, 0]
        // Letter z: [2032, 182, 5, 3, 0, 0, 0, 0, 0, 0]
        val letterCountsEnglish = arrayOf(
            intArrayOf(33735, 6997, 547, 44, 2, 1, 0, 0, 0, 0),
            intArrayOf(9773, 815, 53, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(19491, 2717, 141, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(19196, 2487, 188, 7, 0, 0, 0, 0, 0, 0),
            intArrayOf(45137, 15464, 3071, 287, 12, 0, 0, 0, 0, 0),
            intArrayOf(6948, 877, 24, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(15542, 1645, 167, 9, 0, 0, 0, 0, 0, 0),
            intArrayOf(11844, 852, 18, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(36396, 10132, 1811, 264, 20, 1, 0, 0, 0, 0),
            intArrayOf(974, 6, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(5164, 241, 8, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(24204, 4402, 393, 23, 0, 0, 0, 0, 0, 0),
            intArrayOf(13410, 1425, 60, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(30710, 6644, 768, 44, 2, 0, 0, 0, 0, 0),
            intArrayOf(27453, 6177, 623, 35, 2, 0, 0, 0, 0, 0),
            intArrayOf(14380, 1830, 98, 4, 0, 0, 0, 0, 0, 0),
            intArrayOf(1023, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(33120, 6027, 457, 8, 0, 0, 0, 0, 0, 0),
            intArrayOf(37683, 10743, 1932, 298, 47, 4, 0, 0, 0, 0),
            intArrayOf(30626, 6827, 558, 18, 0, 0, 0, 0, 0, 0),
            intArrayOf(16804, 1406, 72, 7, 0, 0, 0, 0, 0, 0),
            intArrayOf(5414, 177, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(5029, 183, 4, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1544, 4, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(8378, 217, 1, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(2032, 182, 5, 3, 0, 0, 0, 0, 0, 0)
        )

        // Letter a: [356684, 85223, 7652, 519, 17, 0, 0, 0, 0, 0]
        // Letter b: [160176, 16709, 898, 11, 0, 0, 0, 0, 0, 0]
        // Letter c: [193499, 19280, 754, 6, 0, 0, 0, 0, 0, 0]
        // Letter d: [197379, 19462, 611, 26, 0, 0, 0, 0, 0, 0]
        // Letter e: [618852, 475775, 245253, 68822, 8606, 419, 14, 0, 0, 0]
        // Letter f: [129908, 16156, 1228, 35, 0, 0, 0, 0, 0, 0]
        // Letter g: [202311, 26263, 1897, 55, 0, 0, 0, 0, 0, 0]
        // Letter h: [258859, 41444, 2911, 123, 1, 0, 0, 0, 0, 0]
        // Letter i: [333877, 85665, 12203, 983, 69, 0, 0, 0, 0, 0]
        // Letter j: [7808, 41, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter k: [139865, 11136, 379, 1, 0, 0, 0, 0, 0, 0]
        // Letter l: [263769, 48382, 5136, 275, 11, 0, 0, 0, 0, 0]
        // Letter m: [173995, 31400, 3060, 198, 10, 0, 0, 0, 0, 0]
        // Letter n: [418304, 149322, 32267, 4842, 475, 18, 1, 0, 0, 0]
        // Letter o: [181046, 24644, 2602, 172, 13, 0, 0, 0, 0, 0]
        // Letter p: [103009, 15122, 1308, 59, 9, 2, 0, 0, 0, 0]
        // Letter q: [4335, 14, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter r: [428704, 143364, 22738, 1793, 85, 2, 0, 0, 0, 0]
        // Letter s: [399685, 137703, 32224, 5415, 528, 27, 1, 0, 0, 0]
        // Letter t: [398278, 132104, 24672, 2760, 172, 3, 0, 0, 0, 0]
        // Letter u: [256827, 44138, 3246, 72, 0, 0, 0, 0, 0, 0]
        // Letter v: [58989, 1072, 10, 0, 0, 0, 0, 0, 0, 0]
        // Letter w: [68051, 2102, 20, 0, 0, 0, 0, 0, 0, 0]
        // Letter x: [7851, 26, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter y: [11544, 353, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter z: [97785, 5622, 125, 0, 0, 0, 0, 0, 0, 0]

        val letterCountsGerman = arrayOf(
            intArrayOf(356684, 85223, 7652, 519, 17, 0, 0, 0, 0, 0),
            intArrayOf(160176, 16709, 898, 11, 0, 0, 0, 0, 0, 0),
            intArrayOf(193499, 19280, 754, 6, 0, 0, 0, 0, 0, 0),
            intArrayOf(197379, 19462, 611, 26, 0, 0, 0, 0, 0, 0),
            intArrayOf(618852, 475775, 245253, 68822, 8606, 419, 14, 0, 0, 0),
            intArrayOf(129908, 16156, 1228, 35, 0, 0, 0, 0, 0, 0),
            intArrayOf(202311, 26263, 1897, 55, 0, 0, 0, 0, 0, 0),
            intArrayOf(258859, 41444, 2911, 123, 1, 0, 0, 0, 0, 0),
            intArrayOf(333877, 85665, 12203, 983, 69, 0, 0, 0, 0, 0),
            intArrayOf(7808, 41, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(139865, 11136, 379, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(263769, 48382, 5136, 275, 11, 0, 0, 0, 0, 0),
            intArrayOf(173995, 31400, 3060, 198, 10, 0, 0, 0, 0, 0),
            intArrayOf(418304, 149322, 32267, 4842, 475, 18, 1, 0, 0, 0),
            intArrayOf(181046, 24644, 2602, 172, 13, 0, 0, 0, 0, 0),
            intArrayOf(103009, 15122, 1308, 59, 9, 2, 0, 0, 0, 0),
            intArrayOf(4335, 14, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(428704, 143364, 22738, 1793, 85, 2, 0, 0, 0, 0),
            intArrayOf(399685, 137703, 32224, 5415, 528, 27, 1, 0, 0, 0),
            intArrayOf(398278, 132104, 24672, 2760, 172, 3, 0, 0, 0, 0),
            intArrayOf(256827, 44138, 3246, 72, 0, 0, 0, 0, 0, 0),
            intArrayOf(58989, 1072, 10, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(68051, 2102, 20, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(7851, 26, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(11544, 353, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(97785, 5622, 125, 0, 0, 0, 0, 0, 0, 0)
        )
    }
}