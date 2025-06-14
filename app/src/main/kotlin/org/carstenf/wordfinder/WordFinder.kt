/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.carstenf.wordfinder.GameState.PlayerGuessState
import org.carstenf.wordfinder.GameState.TIMER_MODE
import org.carstenf.wordfinder.InfoDialogFragment.Companion.showInfo
import org.carstenf.wordfinder.fireworks.FIREWORK_DISMISS
import org.carstenf.wordfinder.fireworks.FIREWORK_DISMISSED
import org.carstenf.wordfinder.fireworks.FireworksPlayer
import java.io.IOException

class WordFinder : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private val playerResultList by lazy {
        ArrayAdapter(this, R.layout.list_item, R.id.resultText, gameState.playerResultList)
    }

    private val computerResultListView by lazy<ListView> { findViewById(R.id.computerResultsList) }

    val gameState by lazy { ViewModelProvider(this)[GameState::class] }

    private val showAllRow by lazy<View> { findViewById(R.id.showAllRow) }

    private val okButton by lazy<Button> { findViewById(R.id.okButton) }

    private val scoreTextView by lazy<TextView> { findViewById(R.id.scoreTextView) }

    private val countDownView by lazy<TextView> { findViewById(R.id.chronometer1) }

    private var guessButtonEnabledTextColour = 0
    private var showComputerResultsFlag = false

    private val wordDefinitionLookupManager by lazy { WordDefinitionLookupManager(this, gameState) }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOW_COMPUTER_RESULTS_FLAG, showComputerResultsFlag)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        showComputerResults(savedInstanceState.getBoolean(SHOW_COMPUTER_RESULTS_FLAG, false), false)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                val hasNavigationBar = !isGestureNavigationEnabled(this@WordFinder)
                Log.d(TAG, "Navigation Bar: $hasNavigationBar")
                if(hasNavigationBar) {
                    moveTaskToBack(true)
                } else {
                    Log.d(TAG, "Ignore Tiramisu Back")
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val hasNavigationBar = !isGestureNavigationEnabled(this@WordFinder)
                    Log.d(TAG, "Navigation Bar: $hasNavigationBar")
                    if(hasNavigationBar) {
                        moveTaskToBack(true)
                    } else {
                        Log.d(TAG, "Ignore Q Back")
                    }
                }
            })
        }

        okButton.setOnClickListener {
            lifecycleScope.launch {
                okClick()
            }
        }

        findViewById<View>(R.id.showAllButton).setOnClickListener {
            this.solveClick()
        }

        findViewById<View>(R.id.shuffleButton).setOnClickListener {
            this.shuffleClick()
        }

        val playerResultListView = findViewById<ListView>(R.id.playerResultsList)

        gameState.gameLifecycleState.observe(this) {
            @Suppress("CascadeIf") // Warning makes no sense as we don't consider all possible values
            if (it == GameState.GameLifeCycleState.TIMER_FINISHED) {
                disableGuessing()
            } else if (it == GameState.GameLifeCycleState.GAME_OVER) {
                gameState.cancelTimer()
                disableGuessing()
            } else if (
                it == GameState.GameLifeCycleState.UNSOLVABLE) {
                gameState.cancelTimer()
                runOnUiThread {
                    disableGuessing()
                    showUnsolvableDialog(this)
                }
            } else if (it == GameState.GameLifeCycleState.STARTED) {
                enableGuessing()
            }
        }

        try {
            gameState.dictionary = Dictionary(this)
        } catch (e: IOException) {
            throw RuntimeException("Could not create Dictionaries: " + e.message, e)
        }
        wordDefinitionLookupManager.wordLookupError.observe(this) { error: Pair<WordLookupTask, String?>? ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val progressBarView = wordDefinitionLookupManager.wordLookupTaskMap[error?.first?.lookupTaskCounter]
                progressBarView?.visibility = View.GONE
                if (error?.second != null) displayToast(error.second)
            }
        }

        wordDefinitionLookupManager.wordLookupResult.value = null
        wordDefinitionLookupManager.wordLookupResult.observe(this, Observer { result: Pair<WordLookupTask, WordInfo?>? ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val progressBarView = wordDefinitionLookupManager.wordLookupTaskMap[result?.first?.lookupTaskCounter]
                progressBarView?.visibility = View.GONE

                val wordInfo = result?.second ?: return@Observer

                wordDefinitionLookupManager.displayWordDefinition(wordInfo)
            }
        })

        playerResultListView.adapter = playerResultList

        playerResultListView.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>, view: View?, position: Int, _: Long ->
                val selectedItem = parent.getItemAtPosition(position) as Result
                val progressBarView = view?.findViewById<View>(R.id.row_progress)

                wordDefinitionLookupManager.wordDefinitionLookup(
                    selectedItem,
                    progressBarView
                )
            }

        val computerResultList = gameState.computerResultList

        val computerResultListAdapter = ComputerResultListAdapter(this)

        computerResultList.observe(
            this
        ) { list: ArrayList<Result>? ->
            computerResultListAdapter.clear()
            list?.let { computerResultListAdapter.addAll(it) }
            computerResultListAdapter.notifyDataSetChanged()
            updateScore()
        }

        computerResultListView.setAdapter(computerResultListAdapter)

        computerResultListView.setOnItemClickListener { parent: AdapterView<*>, view: View?, position: Int, _: Long ->
            val selectedItem = parent.getItemAtPosition(position) as Result
            val progressBarView = view?.findViewById<View>(R.id.row_progress)

            wordDefinitionLookupManager.wordDefinitionLookup(
                selectedItem,
                progressBarView
            )
        }

        gameState.timerCurrentValue.observe(
            this
        ) { time: Long -> this.updateTimeView(time) }

        val themeArray = theme.obtainStyledAttributes(intArrayOf(android.R.attr.editTextColor))
        try {
            val index = 0
            val defaultColourValue = 0
            guessButtonEnabledTextColour = themeArray.getColor(index, defaultColourValue)
        } finally {
            // Calling recycle() is important. Especially if you use a lot of TypedArrays
            // http://stackoverflow.com/a/13805641/8524
            themeArray.recycle()
        }

        supportFragmentManager.setFragmentResultListener(FIREWORK_DISMISS, this) { _, bundle ->
            if (bundle.getBoolean(FIREWORK_DISMISSED)) {
                showGameWonDialog(this)
            }
        }

        addGestureHandler(this, findViewById(R.id.letterGridView))

        labelDices()

        updateDiceState(-1)
        updateOkButton()

        updateScore()
    }

    private fun enableGuessing() {
        for (button in letterButtons) {
            button.isEnabled = true
            button.setContentDescription("Unavailable Letter Button")
        }
    }

    private fun disableGuessing() {
        gameState.clearGuess()
        updateOkButton()

        for (button in letterButtons) {
            button.isEnabled = false
            button.setContentDescription("Unavailable Letter Button")
        }
    }


    fun isGameOver(): Boolean {
        val state = gameState.gameLifecycleState.value
        return state == GameState.GameLifeCycleState.NOT_STARTED ||
                state == GameState.GameLifeCycleState.UNSOLVABLE ||
                state == GameState.GameLifeCycleState.GAME_OVER ||
                state == GameState.GameLifeCycleState.TIMER_FINISHED
    }


    public override fun onResume() {
        super.onResume()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        gameState.onResume() // This will trigger UI update indirectly if game over
    }

    var showConfirmStartGameDialogVisible= false

    public override fun onPostResume() {
        super.onPostResume()

        if (preferencesChanged) {
            prefs
            if(reshuffleRequired) {
                showRestartRequiredDialog(this)
                reshuffleRequired = false
            }
            preferencesChanged = false
        }

        if (gameState.gameLifecycleState.value == GameState.GameLifeCycleState.NOT_STARTED) {
            if(!showConfirmStartGameDialogVisible)
                showConfirmStartGameDialog(this)
        }
    }

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager
            .getDefaultSharedPreferences(baseContext)

    public override fun onPause() {
        super.onPause()
        gameState.onPause()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var preferencesChanged = false
    private var reshuffleRequired = false

    private val defaultDict by lazy {getString(R.string.default_dict)}

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i(TAG, "Preferences changed for $key")
        preferencesChanged = true
        if (key == "dict_pref" &&
            sharedPreferences.getString("dict_pref", defaultDict) != gameState.dictionaryName) {
            reshuffleRequired = true
        }
        if(key =="threeLetterPref" &&
            sharedPreferences.getBoolean("threeLetterPref", true) != gameState.isAllow3LetterWords) {
            reshuffleRequired = true
        }
    }


    private val prefs: Unit
        get() {
            val prefs = sharedPreferences

            gameState.dictionaryName = prefs.getString("dict_pref", defaultDict)
            gameState.setScoringAlgorithm(prefs.getString("scoring_pref", "count"))

            gameState.setLetterSelector(prefs.getString("rand_dist_pref", "multiLetterFrequency"))

            gameState.isAllow3LetterWords = prefs.getBoolean("threeLetterPref", true)

            gameState.setAutoAddPrefixalWords(
                prefs.getBoolean("autoAddPrefixPref", false)
            )

            if (prefs.getBoolean("countdown_pref", true)) {
                gameState.timerMode = TIMER_MODE.COUNT_DOWN
                val timeStr = prefs.getString("countdown_time_pref", "03:00")!!
                val time = parseTime(timeStr)
                gameState.gameTime = time
            } else {
                gameState.timerMode = TIMER_MODE.STOP_WATCH
                gameState.gameTime = 0
            }
            updateScore()
        }


    private fun updateTimeView(time: Long) {
        if (isFinishing) return

        val m = time / 60
        val s = time % 60
        val ms = "%02d:%02d".format(m, s)
        countDownView.text = ms
        if (time == 0L && gameState.timerMode== TIMER_MODE.COUNT_DOWN &&
            gameState.gameLifecycleState.value != GameState.GameLifeCycleState.NOT_STARTED) {

            if(gameState.gameLifecycleState.value != GameState.GameLifeCycleState.TIMER_FINISHED) {
                gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.TIMER_FINISHED)
            }
            if (!isFinishing)
                showTimeIsUpDialog(this)
        }
    }

    override fun onStart() {
        super.onStart()
        prefs
    }

    private fun updateDiceState(move: Int) {
        if (move >= 0) {
            for (button in letterButtons) {
                button.isEnabled = false
                button.setContentDescription("Unavailable Letter Button")
            }

            for (bid in MOVES[move]) {
                val enabled = gameState.isAvailable(bid)
                letterButtons[bid].isEnabled = enabled
                if (!enabled) {
                    letterButtons[bid].setContentDescription(
                        "Disabled Letter " + gameState.getBoard(
                            bid
                        )
                    )
                }
            }
        } else {
            for (c in 0..15) {
                val l = gameState.getBoard(c)
                val enabled = l != '\u0000' && gameState.isAvailable(c)
                letterButtons[c].isEnabled = enabled
                if (!enabled) {
                    letterButtons[c].setContentDescription("Disabled Letter $l")
                }
            }
        }
    }

    private fun labelDices() {
        for (c in 0..15) {
            val l = gameState.getBoard(c)
            letterButtons[c].setText(l.toString())
            letterButtons[c].setContentDescription("Letter $l")
        }
    }

    fun shuffle() {
        showComputerResults(show = false, animate = false)

        gameState.stopSolving()

        playerResultList.clear()

        gameState.shuffle()

        labelDices()

        gameState.startSolving()

        updateDiceState(gameState.lastMove)
        updateOkButton()
        gameState.startTimer()
        updateScore()
    }

    private fun shuffleClick() {
        showConfirmShuffleDialog(this)
    }

    private val letterButtons by lazy {
        val res = ArrayList<LetterButton>(16)
        for (c in 0..15) {
            val button = this.findViewById<Button>(letterButtonIds[c])
            res+= LetterButton(c, button)
            idToLetterButton.put(letterButtonIds[c], res[c])
            button.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    button.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // Calculate text size based on button height
                    val buttonHeight = button.height
                    val textSize = buttonHeight * 0.2f
                    button.textSize = textSize
                }
            })
        }
        return@lazy res
    }

    private val idToLetterButton = SparseArray<LetterButton?>()

    fun letterClick(view: View) {
        val pressedButton = checkNotNull(idToLetterButton[view.id])

        if (!pressedButton.isEnabled) return

        val move = pressedButton.pos
        gameState.play(move)

        updateOkButton()
        updateDiceState(move)
    }

    suspend fun okClick() {
        if(isGameOver())
            return

        var guess = gameState.currentGuess

        var guessVal = gameState.validatePlayerGuess(guess)
        if (guessVal.state == PlayerGuessState.GUESS_VALID) {
            insertPlayerResult(guessVal.guess)
            if (gameState.autoAddPrefixalWords()) {
                testAndAddPrefixWords(guess)
            }
        } else {
            guess = guess.replace("Q".toRegex(), "QU")
            guessVal = gameState.validatePlayerGuess(guess)
            if (guessVal.state == PlayerGuessState.GUESS_VALID) {
                insertPlayerResult(guessVal.guess)
                if (gameState.autoAddPrefixalWords()) {
                    testAndAddPrefixWords(guess)
                }
            } else {
                val text: String = when (guessVal.state) {
                    PlayerGuessState.ALREADY_FOUND -> getString(R.string.WordAlreadyFound)
                    PlayerGuessState.NOT_IN_DICTIONARY -> getString(R.string.WordNotInDictionary)
                    PlayerGuessState.TOO_SHORT -> getString(R.string.WordGuessTooShort)
                    else -> "" // Can't happen due to if above but IDE can't figure it out
                }

                val context = applicationContext
                val toast = Toast.makeText(context, "\"${guess.replace("QUU", "Q(u)U")}\" $text", Toast.LENGTH_SHORT)
                toast.show()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }

            }
        }

        gameState.clearGuess()
        updateScore()
        updateDiceState(-1)
        updateOkButton()
    }


    private fun highlightFirstMatchingItem(searchText: String) {
        // Get the adapter from the ListView
        val adapter = computerResultListView.adapter as? ArrayAdapter<*>

        // Check if the adapter is not null and has items
        if (adapter != null && adapter.count > 0) {
            // Iterate through the items in the adapter
            for (i in 0 until adapter.count) {
                val result = adapter.getItem(i) as Result

                // Check if the result's text matches the search text
                if (result.result.displayText == searchText) {
                    // Modify the Result object to indicate it should be highlighted
                    result.isHighlighted = true

                    // Notify the adapter that the data has changed
                    adapter.notifyDataSetChanged()

                    // Break the loop after finding the first match
                    break
                }
            }
        }
    }

    private fun insertPlayerResult(guess: Dictionary.WordInfoData) {
        playerResultList.insert(Result(guess), 0)
        highlightFirstMatchingItem(guess.displayText)
    }

    private suspend fun testAndAddPrefixWords(word: String) {
        var localWord = word
        while (localWord.isNotEmpty()) {
            localWord = localWord.substring(0, localWord.length - 1) // Remove the last character

            val result = gameState.validatePlayerGuess(localWord)
            when (result.state) {
                PlayerGuessState.ALREADY_FOUND, PlayerGuessState.NOT_IN_DICTIONARY -> continue
                PlayerGuessState.TOO_SHORT -> return
                PlayerGuessState.GUESS_VALID -> insertPlayerResult(result.guess)
            }
        }
    }

    private fun updateOkButton() {
        val currentGuess = gameState.currentGuess

        okButton.text = currentGuess
        if (currentGuess.isEmpty()) {
            okButton.visibility = View.INVISIBLE
        } else {
            okButton.visibility = View.VISIBLE
            okButton.contentDescription =
                "Current guess: " + (gameState.currentGuess.ifBlank { "empty" })
            val minLength = if (gameState.isAllow3LetterWords) 3 else 4
            val enabled = gameState.currentGuess.length >= minLength
            if (enabled) {
                okButton.setTextColor("#000000".toColorInt())
            } else {
                okButton.setTextColor("#FA1616".toColorInt())
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScore() {
        scoreTextView.text =
            ("${gameState.playerScore}/${gameState.computerScore} ${gameState.dictionaryCountryCode()}")

        if(gameState.solveFinished && gameState.computerScore > 0 &&
            gameState.playerScore == gameState.computerScore) {
            gameState.countUpTimer?.cancel()
            showFirework()
        }
    }

    private fun showFirework() {
        FireworksPlayer.show(supportFragmentManager, durationSeconds = 8)
    }

    private fun showComputerResults(show: Boolean, animate: Boolean) {
        this.showComputerResultsFlag = show

        if (show) {
            computerResultListView.setSelection(0)
            if(animate) {
                slideUpAndHide(showAllRow, computerResultListView)
            } else {
                computerResultListView.visibility = View.VISIBLE
                showAllRow.visibility = View.GONE
            }
        } else {
            showAllRow.visibility = View.VISIBLE
            computerResultListView.visibility = View.GONE
        }
    }

    private fun solveClick() {
        if(gameState.gameLifecycleState.value != GameState.GameLifeCycleState.GAME_OVER) {
            gameState.gameLifecycleState.postValue(GameState.GameLifeCycleState.GAME_OVER)
        }
        showComputerResults(show = true, animate = true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_info -> {
                val fragmentManager = supportFragmentManager
                showInfo(fragmentManager, gameState)
                return true
            }
            R.id.menu_item_prefs -> {
                showPreferences()
                return true
            }
            R.id.menu_item_shuffle -> {
                shuffleClick()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun showPreferences() {
        val settingsActivity = Intent(
            baseContext,
            WordFinderPreferences::class.java
        )
        startActivity(settingsActivity)
    }

    private fun displayToast(text: String?) {
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        const val TAG: String = "CF_WF"

        val MOVES: Array<IntArray> = arrayOf(
            intArrayOf(1, 4, 5),
            intArrayOf(0, 2, 4, 5, 6),
            intArrayOf(1, 3, 5, 6, 7),
            intArrayOf(2, 6, 7),
            intArrayOf(0, 1, 5, 8, 9),
            intArrayOf(0, 1, 2, 4, 6, 8, 9, 10),
            intArrayOf(1, 2, 3, 5, 7, 9, 10, 11),
            intArrayOf(2, 3, 6, 10, 11),
            intArrayOf(4, 5, 9, 12, 13),
            intArrayOf(4, 5, 6, 8, 10, 12, 13, 14),
            intArrayOf(5, 6, 7, 9, 11, 13, 14, 15),
            intArrayOf(6, 7, 10, 14, 15),
            intArrayOf(8, 9, 13),
            intArrayOf(8, 9, 10, 12, 14),
            intArrayOf(9, 10, 11, 13, 15),
            intArrayOf(10, 11, 14)
        )
        private const val SHOW_COMPUTER_RESULTS_FLAG = "SHOW_COMPUTER_RESULTS_FLAG"

        private val letterButtonIds = intArrayOf(
            R.id.button01, R.id.button02,
            R.id.button03, R.id.button04, R.id.button11, R.id.button12,
            R.id.button13, R.id.button14, R.id.button21, R.id.button22,
            R.id.button23, R.id.button24, R.id.button31, R.id.button32,
            R.id.button33, R.id.button34
        )
    }
}