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
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.carstenf.wordfinder.GameState.GameLifeCycleState.*
import org.carstenf.wordfinder.GameState.PlayerGuessState
import org.carstenf.wordfinder.GameState.TIMER_MODE
import org.carstenf.wordfinder.dictionary.Dictionary
import org.carstenf.wordfinder.dictionary.WordDefinitionLookupManager
import org.carstenf.wordfinder.dictionary.WordInfo
import org.carstenf.wordfinder.dictionary.WordLookupTask
import org.carstenf.wordfinder.gui.InfoDialogFragment.Companion.showInfo
import org.carstenf.wordfinder.fireworks.FIREWORK_DISMISS
import org.carstenf.wordfinder.fireworks.FIREWORK_DISMISSED
import org.carstenf.wordfinder.fireworks.FireworksPlayer
import org.carstenf.wordfinder.gui.AppCompatLetterButton
import org.carstenf.wordfinder.gui.BackGestureBlockingTableLayout
import org.carstenf.wordfinder.gui.ComputerResultListAdapter
import org.carstenf.wordfinder.gui.drawConnectionsBetweenButtons
import org.carstenf.wordfinder.util.addGestureHandler
import org.carstenf.wordfinder.util.isGestureNavigationEnabled
import org.carstenf.wordfinder.util.parseTime
import org.carstenf.wordfinder.util.showConfirmShuffleDialog
import org.carstenf.wordfinder.util.showConfirmStartGameDialog
import org.carstenf.wordfinder.util.showGameWonDialog
import org.carstenf.wordfinder.util.showRestartRequiredDialog
import org.carstenf.wordfinder.util.showTableDialog
import org.carstenf.wordfinder.util.showTimeIsUpDialog
import org.carstenf.wordfinder.util.showUnsolvableDialog
import org.carstenf.wordfinder.util.slideUpAndHide
import java.io.IOException
import kotlin.math.max

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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.main)

        val rootView = findViewById<View>(R.id.root)
        if(rootView!=null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                // Individual padding values (in pixels)
                val paddingLeft = view.paddingLeft    // or paddingStart
                val paddingTop = view.paddingTop
                val paddingRight = view.paddingRight  // or paddingEnd
                val paddingBottom = view.paddingBottom

                view.setPadding(
                    paddingLeft,  // Left edge swipe area
                    max(systemBars.top, paddingTop),       // Status bar
                    paddingRight, // Right edge swipe area
                    max(systemBars.bottom, paddingBottom)     // Traditional nav bar OR gesture pill
                )
                insets
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                val hasNavigationBar = !isGestureNavigationEnabled(this@WordFinder)
                Log.d(TAG, "Navigation Bar: $hasNavigationBar") // NON-NLS
                if(hasNavigationBar) {
                    moveTaskToBack(true)
                } else {
                    Log.d(TAG, "Ignore Tiramisu Back") // NON-NLS
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val hasNavigationBar = !isGestureNavigationEnabled(this@WordFinder)
                    Log.d(TAG, "Navigation Bar: $hasNavigationBar") // NON-NLS
                    if(hasNavigationBar) {
                        moveTaskToBack(true)
                    } else {
                        Log.d(TAG, "Ignore Q Back") // NON-NLS
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

        gameState.gameLifecycleState.observe(this) { onGameStateChanged(it) }

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

    private fun onGameStateChanged(state: GameState.GameLifeCycleState) {
        when (state) {
            TIMER_FINISHED -> {
                disableGuessing()
            }
            GAME_OVER -> {
                gameState.cancelTimer()
                disableGuessing()
            }
            UNSOLVABLE -> {
                gameState.cancelTimer()
                runOnUiThread {
                    disableGuessing()
                    showUnsolvableDialog(this)
                }
            }
            STARTED -> {
                enableGuessing()
            }

            NOT_STARTED -> disableGuessing()
            TIMER_STARTED -> enableGuessing()
        }
    }


    internal fun enableGuessing() {
        for (button in letterButtons) {
            button.isChecked = false
            button.setContentDescription(getString(R.string.letter_button, button.text))
        }
        setHintVisibility(true)
    }

    private fun clearGuess() {
        gameState.clearGuess()
        updateOkButton()
        updateLetterButtonOverlay()
    }

    internal fun disableGuessing() {
        clearGuess()
        for (button in letterButtons) {
            button.isChecked = true
            button.setContentDescription(getString(R.string.unavailable_letter_button, button.text))
        }
        setHintVisibility(false)
    }

    private var showHint = false

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_item_hint)?.isVisible = showHint
        return super.onPrepareOptionsMenu(menu)
    }

    fun setHintVisibility(visible: Boolean) {
        showHint = visible
        invalidateOptionsMenu()
    }

    fun isGameOver(): Boolean {
        val state = gameState.gameLifecycleState.value
        return state == NOT_STARTED ||
                state == UNSOLVABLE ||
                state == GAME_OVER ||
                state == TIMER_FINISHED
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
            reloadPreferences()
            if(reshuffleRequired) {
                showRestartRequiredDialog(this)
                reshuffleRequired = false
            }
            preferencesChanged = false
        }

        if (gameState.gameLifecycleState.value == NOT_STARTED) {
            if(!showConfirmStartGameDialogVisible)
                showConfirmStartGameDialog(this)
        }

        updateButtonEnabledStatus()
    }

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager
            .getDefaultSharedPreferences(baseContext)

    public override fun onPause() {
        super.onPause()
        clearGuess()
        gameState.onPause()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var preferencesChanged = false
    private var reshuffleRequired = false

    private val defaultDict by lazy {getString(R.string.default_dict)}

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i(TAG, "Preferences changed for $key") // NON-NLS
        preferencesChanged = true
        if (key == "dict_pref" && // NON-NLS
            sharedPreferences.getString("dict_pref", defaultDict) != gameState.dictionaryName) { // NON-NLS
            reshuffleRequired = true
        }
        if(key =="threeLetterPref" &&
            sharedPreferences.getBoolean("threeLetterPref", true) != gameState.isAllow3LetterWords) {
            reshuffleRequired = true
        }
        if(key =="countdown_pref") { // NON-NLS
            reshuffleRequired = true
        }
    }


    private fun reloadPreferences()
    {
        val prefs = sharedPreferences

        gameState.dictionaryName = prefs.getString("dict_pref", defaultDict) // NON-NLS
        gameState.setScoringAlgorithm(prefs.getString("scoring_pref", "count")) // NON-NLS

        gameState.setLetterSelector(prefs.getString("rand_dist_pref", "multiLetterFrequency")) // NON-NLS

        gameState.isAllow3LetterWords = prefs.getBoolean("threeLetterPref", true)

        gameState.setAutoAddPrefixalWords(
            prefs.getBoolean("autoAddPrefixPref", false)
        )

        if (prefs.getBoolean("countdown_pref", true)) { // NON-NLS
            gameState.timerMode = TIMER_MODE.COUNT_DOWN
            val timeStr = prefs.getString("countdown_time_pref", "03:00")!! // NON-NLS
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

        var m = time / 60
        val s = time % 60
        if(m<60) {
            val ms = "%02d:%02d".format(m, s) // NON-NLS
            countDownView.text = ms
        } else {
            val h = m / 60
            m = m % 60
            val ms = "%02d:%02d:%02d".format(h, m, s) // NON-NLS
            countDownView.text = ms
        }

        if (time == 0L && gameState.timerMode== TIMER_MODE.COUNT_DOWN &&
            gameState.gameLifecycleState.value != NOT_STARTED
        ) {

            if (gameState.gameLifecycleState.value != TIMER_FINISHED) {
                gameState.gameLifecycleState.postValue(TIMER_FINISHED)
            }
            if (!isFinishing) {
                showTimeIsUpDialog(this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        reloadPreferences()
    }

    private fun updateDiceState(move: Int) {
        if (move >= 0) {
            for (button in letterButtons) {
                button.isChecked = true
                button.setContentDescription(getString(R.string.unavailable_letter_button, button.text))
            }

            for (bid in MOVES[move]) {
                val enabled = gameState.isAvailable(bid)
                letterButtons[bid].isChecked = !enabled
                if (!enabled) {
                    letterButtons[bid].setContentDescription(getString(R.string.unavailable_letter_button, letterButtons[bid].text))
                }
            }
        } else {
            for (c in 0..15) {
                val l = gameState.getBoard(c)
                val enabled = l != '\u0000' && gameState.isAvailable(c)
                letterButtons[c].isChecked = !enabled
                if (!enabled) {
                    letterButtons[c].setContentDescription(getString(R.string.unavailable_letter_button, letterButtons[c].text))
                }
            }
        }
        updateLetterButtonOverlay()
    }

    private fun updateLetterButtonOverlay() {
        val buttons = mutableListOf<AppCompatButton>()
        for(m in gameState.moves) {
            buttons.add(this.findViewById<AppCompatButton>(letterButtonIds[m]))
        }
        drawConnectionsBetweenButtons(
            this.findViewById<BackGestureBlockingTableLayout>(R.id.letterGridView),
            buttons
        )
    }

    private fun labelDices() {
        for (c in 0..15) {
            val l = gameState.getBoard(c)
            letterButtons[c].text = l.toString()
            letterButtons[c].setContentDescription(getString(R.string.letter_button, letterButtons[c].text))
        }
    }

    fun shuffle() {
        showComputerResults(show = false, animate = false)

        gameState.stopSolving()

        playerResultList.clear()

        gameState.shuffle()

        labelDices()

        gameState.startSolving()

        updateDiceState(-1)
        updateOkButton()
        gameState.startTimer()
        updateScore()
    }

    private fun shuffleClick() {
        clearGuess()
        showConfirmShuffleDialog(this)
    }

    private val letterButtons by lazy {
        val res = ArrayList<AppCompatLetterButton>(16)
        for (c in 0..15) {
            val button = this.findViewById<AppCompatLetterButton>(letterButtonIds[c])
            button.pos = c
            res+= button
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

    private val idToLetterButton = SparseArray<AppCompatLetterButton?>()

    fun onLetterClick(view: View) {
        val pressedButton = checkNotNull(idToLetterButton[view.id])
        var move = pressedButton.pos

        if (pressedButton.isChecked) {
            if(move !in gameState.moves) {
                return
            }
            gameState.undo(move)
            move = gameState.lastMove()
        } else {
            gameState.play(move)
        }
        updateDiceState(move)
        updateOkButton()
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
            guess = guess.replace("Q".toRegex(), "QU") // NON-NLS
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
                val toast = Toast.makeText(context, "\"${guess.replace("QUU", "Q(u)U")}\" $text", Toast.LENGTH_SHORT) // NON-NLS
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
        playerResultList.sort {
            object1: Result, object2: Result ->
            val s1 = object1.toString()
            val s2 = object2.toString()
            s1.compareTo(s2)
        }
        playerResultList.notifyDataSetChanged()
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
            if(gameState.currentGuess.isBlank()) {
                okButton.contentDescription = getString(R.string.current_guess,  gameState.currentGuess)
            } else {
                okButton.contentDescription = getString(R.string.current_guess_empty)
            }
            val minLength = if (gameState.isAllow3LetterWords) 3 else 4
            val enabled = gameState.currentGuess.length >= minLength
            if (enabled) {
                okButton.setTextColor("#000000".toColorInt()) // NON-NLS
            } else {
                okButton.setTextColor("#FA1616".toColorInt()) // NON-NLS
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
        if(gameState.gameLifecycleState.value != GAME_OVER) {
            gameState.gameLifecycleState.postValue(GAME_OVER)
        }
        showComputerResults(show = true, animate = true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.option_menu, menu)
        return true
    }

    fun updateButtonEnabledStatus(){
        if (gameState.gameLifecycleState.value == STARTED ||
            gameState.gameLifecycleState.value == TIMER_STARTED
        ) {
            enableGuessing()
        } else {
            disableGuessing()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_info -> {
                val fragmentManager = supportFragmentManager
                clearGuess()
                showInfo(fragmentManager, gameState)
                updateButtonEnabledStatus()
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
            R.id.menu_item_hint -> {
                displayHint()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun displayHint() {
        showTableDialog(
            supportFragmentManager,
            getString(R.string.number_of_words_still_to_be_found_by_word_length),
            listOf(
                getString(R.string.word_length),
                getString(R.string.number_of_words)
            ),
            getHintTableData(),
            10
        )
    }

    private fun getHintTableData(): List<List<String>> {
        val currentGuesses = gameState.playerResultList
        val solution = gameState.computerResultList

        val solutionList = solution.value ?: return emptyList()
        val missingResults = solutionList.filter { solutionResult ->
            // Check if no item in currentGuesses has the same text
            currentGuesses.none { guess -> guess.result.text == solutionResult.result.text }
        }

        val result = missingResults
            .groupBy { it.result.text.length }  // Group by word length
            .mapValues { it.value.size } // Convert to counts
            .filter { it.value > 0 }     // Remove lengths with zero counts
            .entries
            .sortedBy { it.key }         // Sort by word length
            .map { listOf(it.key.toString(), it.value.toString()) } // Convert to List<String>
        return result
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
        const val TAG: String = "CF_WF" // NON-NLS

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
        private const val SHOW_COMPUTER_RESULTS_FLAG = "SHOW_COMPUTER_RESULTS_FLAG" // NON-NLS

        private val letterButtonIds = intArrayOf(
            R.id.button01, R.id.button02,
            R.id.button03, R.id.button04, R.id.button11, R.id.button12,
            R.id.button13, R.id.button14, R.id.button21, R.id.button22,
            R.id.button23, R.id.button24, R.id.button31, R.id.button32,
            R.id.button33, R.id.button34
        )
    }
}