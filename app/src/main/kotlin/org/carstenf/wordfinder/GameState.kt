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

    private val wordInfoCache: WordInfoCache = WordInfoCache()

	val computerResultList: MutableLiveData<ArrayList<Result>?> = MutableLiveData(ArrayList())
	val playerResultList: ArrayList<Result> = ArrayList()

	val countDownTimerCurrentValue: MutableLiveData<Long> = MutableLiveData(-1L)

	val wordLookupResult: MutableLiveData<WordInfo?> = MutableLiveData(null)
	val wordLookupError: MutableLiveData<String?> = MutableLiveData(null)

    fun getBoard(move: Int): Char {
        return board[move]
    }

    private val playerTaken = BooleanArray(16)

    var isTimeUp: Boolean = false

    private var solver: SolveTask? = null

    fun shuffle() {
        clearGuess()
        isTimeUp = false
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

        if (dictionaryName.equals("german", ignoreCase = true)) {
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

    fun getWordInfoFromCache(word: String, language: String): WordInfo? {
        return wordInfoCache.get(word, language)
    }

    fun processWordLookupError(word: String, language: String, error: String?) {
        wordInfoCache.put(WordInfo(word, language, null, null))
        wordLookupError.postValue(error)
    }

    fun processWordLookupResult(wordInfo: WordInfo) {
        wordInfoCache.put(wordInfo)
        wordLookupResult.postValue(wordInfo)
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

    fun validatePlayerGuess(guess: String): PlayerGuessState? {
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
        countDownTimer?.cancel()

        if (countDownTime < 0) return

        countDownTimerCurrentValue.postValue(countDownTime)

        countDownTimer = object : CountDownTimer(countDownTime, 1000) {
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

    val playerScore: Int
        get() = getScore(playerResultList)

    fun hasGameStarted(): Boolean {
        return board[0].code != 0
    }

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
            0.08167, 0.09659, 0.12441, 0.16694,
            0.29396, 0.31624, 0.33639, 0.39733, 0.46699, 0.46852, 0.47624,
            0.51649, 0.54055, 0.60804, 0.68311, 0.7024, 0.70335, 0.76322,
            0.82649, 0.91705, 0.94463, 0.95441, 0.97801, 0.97951, 0.99925, 1.0
        )

        private val letterFreqProbGerman = doubleArrayOf(
            0.068050,
            0.086910,
            0.114230,
            0.164990,
            0.339010,
            0.355570,
            0.385660,
            0.431430,
            0.496930,
            0.499610,
            0.513780,
            0.548150,
            0.573490,
            0.671250,
            0.699405,
            0.706105,
            0.706285,
            0.776315,
            0.852085,
            0.913625,
            0.960260,
            0.968720,
            0.987930,
            0.988270,
            0.988660,
            1.000000
        )
    }
}
