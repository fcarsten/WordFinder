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

        computerResultList.value?.clear()
        computerResultList.postValue(computerResultList.value)
    }

    private fun pickRandomLetter(letterCounts: IntArray): Char {
        var letterCountMatrix = letterCountsEnglish

        if (dictionaryName.equals("german", ignoreCase = true) ||
            dictionaryName.equals("german_wiki", ignoreCase = true) ||
            dictionaryName.equals("german_simple", ignoreCase = true)) {
            letterCountMatrix = letterCountsGerman
        }

        var totalCount = 0
        for (k in 0..25) {
            totalCount += letterCountMatrix[k][letterCounts[k]]
        }

        var r = Math.random()*totalCount
        var i = 0

        while ( letterCountMatrix[i][letterCounts[i]] < r) {
            r -= letterCountMatrix[i][letterCounts[i]]
            i += 1
        }

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

    val gameLifecycleState: MutableLiveData<GameLifeCycleState> = MutableLiveData(GameLifeCycleState.NOT_STARTED)

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

        if (guess.length < minLength) return PlayerGuessResult(Dictionary.WordInfoData(guess), PlayerGuessState.TOO_SHORT)

        val lookupResult = dictionary.lookup(guess, dictionaryName)
            ?: return PlayerGuessResult(Dictionary.WordInfoData(guess),PlayerGuessState.NOT_IN_DICTIONARY)

        for (result in playerResultList) {
            if (result.toString()
                    .equals(lookupResult.displayText, ignoreCase = true)
            ) return PlayerGuessResult(Dictionary.WordInfoData(guess),PlayerGuessState.ALREADY_FOUND)
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
        countDownTimerCurrentValue.postValue(time/1000)

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

    fun onSolveFinished() {
        if(computerResultList.value?.size == 0) {
            countDownTimer?.cancel()
            gameLifecycleState.postValue(GameLifeCycleState.UNSOLVABLE)
        }
    }

    val playerScore: Int
        get() = getScore(playerResultList)

    internal enum class ScoreAlgorithm {
        COUNT, VALUE
    }

    val computerScore: Int
        get() = getScore(computerResultList.value)

    companion object {
        // Letter a: [26738, 6450, 503, 42, 1, 1, 0, 0, 0, 0]
        // Letter b: [8958, 762, 51, 2, 0, 0, 0, 0, 0, 0]
        // Letter c: [16774, 2576, 141, 0, 0, 0, 0, 0, 0, 0]
        // Letter d: [16709, 2299, 181, 7, 0, 0, 0, 0, 0, 0]
        // Letter e: [29673, 12393, 2784, 275, 12, 0, 0, 0, 0, 0]
        // Letter f: [6071, 853, 23, 1, 0, 0, 0, 0, 0, 0]
        // Letter g: [13897, 1478, 158, 9, 0, 0, 0, 0, 0, 0]
        // Letter h: [10992, 834, 18, 0, 0, 0, 0, 0, 0, 0]
        // Letter i: [26264, 8321, 1547, 244, 19, 1, 0, 0, 0, 0]
        // Letter j: [968, 6, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter k: [4923, 233, 6, 2, 0, 0, 0, 0, 0, 0]
        // Letter l: [19802, 4009, 370, 23, 0, 0, 0, 0, 0, 0]
        // Letter m: [11985, 1365, 58, 2, 0, 0, 0, 0, 0, 0]
        // Letter n: [24066, 5876, 724, 42, 2, 0, 0, 0, 0, 0]
        // Letter o: [21276, 5554, 588, 33, 2, 0, 0, 0, 0, 0]
        // Letter p: [12550, 1732, 94, 4, 0, 0, 0, 0, 0, 0]
        // Letter q: [1023, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter r: [27093, 5570, 449, 8, 0, 0, 0, 0, 0, 0]
        // Letter s: [26940, 8811, 1634, 251, 43, 4, 0, 0, 0, 0]
        // Letter t: [23799, 6269, 540, 18, 0, 0, 0, 0, 0, 0]
        // Letter u: [15398, 1334, 65, 7, 0, 0, 0, 0, 0, 0]
        // Letter v: [5237, 177, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter w: [4846, 179, 4, 0, 0, 0, 0, 0, 0, 0]
        // Letter x: [1540, 4, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter y: [8161, 216, 1, 0, 0, 0, 0, 0, 0, 0]
        // Letter z: [1850, 177, 2, 3, 0, 0, 0, 0, 0, 0]
        val letterCountsEnglish = arrayOf(
            intArrayOf(26738, 6450, 503, 42, 1, 1, 0, 0, 0, 0),
            intArrayOf(8958, 762, 51, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(16774, 2576, 141, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(16709, 2299, 181, 7, 0, 0, 0, 0, 0, 0),
            intArrayOf(29673, 12393, 2784, 275, 12, 0, 0, 0, 0, 0),
            intArrayOf(6071, 853, 23, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(13897, 1478, 158, 9, 0, 0, 0, 0, 0, 0),
            intArrayOf(10992, 834, 18, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(26264, 8321, 1547, 244, 19, 1, 0, 0, 0, 0),
            intArrayOf(968, 6, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(4923, 233, 6, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(19802, 4009, 370, 23, 0, 0, 0, 0, 0, 0),
            intArrayOf(11985, 1365, 58, 2, 0, 0, 0, 0, 0, 0),
            intArrayOf(24066, 5876, 724, 42, 2, 0, 0, 0, 0, 0),
            intArrayOf(21276, 5554, 588, 33, 2, 0, 0, 0, 0, 0),
            intArrayOf(12550, 1732, 94, 4, 0, 0, 0, 0, 0, 0),
            intArrayOf(1023, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(27093, 5570, 449, 8, 0, 0, 0, 0, 0, 0),
            intArrayOf(26940, 8811, 1634, 251, 43, 4, 0, 0, 0, 0),
            intArrayOf(23799, 6269, 540, 18, 0, 0, 0, 0, 0, 0),
            intArrayOf(15398, 1334, 65, 7, 0, 0, 0, 0, 0, 0),
            intArrayOf(5237, 177, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(4846, 179, 4, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1540, 4, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(8161, 216, 1, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1850, 177, 2, 3, 0, 0, 0, 0, 0, 0)
        )

        // Letter a: [271461, 77571, 7133, 502, 17, 0, 0, 0, 0, 0]
        // Letter b: [143467, 15811, 887, 11, 0, 0, 0, 0, 0, 0]
        // Letter c: [174219, 18526, 748, 6, 0, 0, 0, 0, 0, 0]
        // Letter d: [177917, 18851, 585, 26, 0, 0, 0, 0, 0, 0]
        // Letter e: [143077, 230522, 176431, 60216, 8187, 405, 14, 0, 0, 0]
        // Letter f: [113752, 14928, 1193, 35, 0, 0, 0, 0, 0, 0]
        // Letter g: [176048, 24366, 1842, 55, 0, 0, 0, 0, 0, 0]
        // Letter h: [217415, 38533, 2788, 122, 1, 0, 0, 0, 0, 0]
        // Letter i: [248212, 73462, 11220, 914, 69, 0, 0, 0, 0, 0]
        // Letter j: [7767, 41, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter k: [128729, 10757, 378, 1, 0, 0, 0, 0, 0, 0]
        // Letter l: [215387, 43246, 4861, 264, 11, 0, 0, 0, 0, 0]
        // Letter m: [142595, 28340, 2862, 188, 10, 0, 0, 0, 0, 0]
        // Letter n: [268982, 117055, 27425, 4367, 457, 17, 1, 0, 0, 0]
        // Letter o: [156402, 22042, 2430, 159, 13, 0, 0, 0, 0, 0]
        // Letter p: [87887, 13814, 1249, 50, 7, 2, 0, 0, 0, 0]
        // Letter q: [4321, 14, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter r: [285340, 120626, 20945, 1708, 83, 2, 0, 0, 0, 0]
        // Letter s: [261982, 105479, 26809, 4887, 501, 26, 1, 0, 0, 0]
        // Letter t: [266174, 107432, 21912, 2588, 169, 3, 0, 0, 0, 0]
        // Letter u: [212689, 40892, 3174, 72, 0, 0, 0, 0, 0, 0]
        // Letter v: [57917, 1062, 10, 0, 0, 0, 0, 0, 0, 0]
        // Letter w: [65949, 2082, 20, 0, 0, 0, 0, 0, 0, 0]
        // Letter x: [7825, 26, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter y: [11191, 353, 0, 0, 0, 0, 0, 0, 0, 0]
        // Letter z: [92163, 5497, 125, 0, 0, 0, 0, 0, 0, 0]
        val letterCountsGerman = arrayOf(
            intArrayOf(271461, 77571, 7133, 502, 17, 0, 0, 0, 0, 0),
            intArrayOf(143467, 15811, 887, 11, 0, 0, 0, 0, 0, 0),
            intArrayOf(174219, 18526, 748, 6, 0, 0, 0, 0, 0, 0),
            intArrayOf(177917, 18851, 585, 26, 0, 0, 0, 0, 0, 0),
            intArrayOf(143077, 230522, 176431, 60216, 8187, 405, 14, 0, 0, 0),
            intArrayOf(113752, 14928, 1193, 35, 0, 0, 0, 0, 0, 0),
            intArrayOf(176048, 24366, 1842, 55, 0, 0, 0, 0, 0, 0),
            intArrayOf(217415, 38533, 2788, 122, 1, 0, 0, 0, 0, 0),
            intArrayOf(248212, 73462, 11220, 914, 69, 0, 0, 0, 0, 0),
            intArrayOf(7767, 41, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(128729, 10757, 378, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(215387, 43246, 4861, 264, 11, 0, 0, 0, 0, 0),
            intArrayOf(142595, 28340, 2862, 188, 10, 0, 0, 0, 0, 0),
            intArrayOf(268982, 117055, 27425, 4367, 457, 17, 1, 0, 0, 0),
            intArrayOf(156402, 22042, 2430, 159, 13, 0, 0, 0, 0, 0),
            intArrayOf(87887, 13814, 1249, 50, 7, 2, 0, 0, 0, 0),
            intArrayOf(4321, 14, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(285340, 120626, 20945, 1708, 83, 2, 0, 0, 0, 0),
            intArrayOf(261982, 105479, 26809, 4887, 501, 26, 1, 0, 0, 0),
            intArrayOf(266174, 107432, 21912, 2588, 169, 3, 0, 0, 0, 0),
            intArrayOf(212689, 40892, 3174, 72, 0, 0, 0, 0, 0, 0),
            intArrayOf(57917, 1062, 10, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(65949, 2082, 20, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(7825, 26, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(11191, 353, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(92163, 5497, 125, 0, 0, 0, 0, 0, 0, 0)
        )    }
}
