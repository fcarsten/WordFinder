/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import org.carstenf.wordfinder.GameState.PlayerGuessState
import org.carstenf.wordfinder.InfoDialogFragment.Companion.showInfo
import org.carstenf.wordfinder.Util.isNetworkAvailable
import java.io.IOException
import java.util.Locale

class WordFinder : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var playerResultList: ArrayAdapter<Result>? = null

    private var computerResultListView: ListView? = null


    private var okButton: Button? = null

    private var gameState: GameState? = null

    private var showAllRow: View? = null

    private var scoreTextView: TextView? = null

    private var countDownView: TextView? = null
    private var guessButtonEnabledTextColour = 0
    private var showComputerResultsFlag = false

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(SHOW_COMPUTER_RESULTS_FLAG, showComputerResultsFlag)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        showComputerResults(savedInstanceState.getBoolean(SHOW_COMPUTER_RESULTS_FLAG, false))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                Log.d(TAG, "Ignore Tiramisu Back")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d(TAG, "Ignore Q Back")
                    // Do nothing (blocks the back gesture)
                }
            })
        }

        okButton = findViewById(R.id.okButton)
        okButton?.setOnClickListener { this.okClick() }

        this.showAllRow = findViewById(R.id.showAllRow)
        findViewById<View>(R.id.showAllButton).setOnClickListener {
            this.solveClick()
        }

        findViewById<View>(R.id.shuffleButton).setOnClickListener {
            this.shuffleClick()
        }

        val playerResultListView = findViewById<ListView>(R.id.playerResultsList)
        this.computerResultListView = findViewById(R.id.computerResultsList)

        countDownView = findViewById(R.id.chronometer1)
        countDownView?.visibility = View.INVISIBLE

        scoreTextView = findViewById(R.id.scoreTextView)
        for (c in 0..15) {
            val button = this.findViewById<Button>(letterButtonIds[c])
            letterButtons[c] = LetterButton(c, button)
            idToLetterButton.put(letterButtonIds[c], letterButtons[c])
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

        gameState = ViewModelProvider(this)[GameState::class.java]
        try {
            gameState!!.dictionary = Dictionary(this)
        } catch (e: IOException) {
            throw RuntimeException("Could not create Dictionaries: " + e.message, e)
        }

        gameState!!.wordLookupError.observe(this) { text: String? ->
            if (text != null) displayToast(text, Toast.LENGTH_SHORT)
        }

        gameState!!.wordLookupResult.value = null
        gameState!!.wordLookupResult.observe(this, Observer { wordInfo: WordInfo? ->
            if(wordInfo==null) return@Observer

            if (wordInfo.wordDefinition.isNullOrBlank()) {
                displayToast("Definition not found for: " + wordInfo.word, Toast.LENGTH_SHORT)
            } else {
                displayWordDefinition(wordInfo.wordDefinition!!)
            }
        })

        playerResultList = ArrayAdapter(
            this, R.layout.list_item,
            gameState!!.playerResultList
        )
        playerResultListView.adapter = playerResultList

        playerResultListView.onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
                wordDefinitionLookup(
                    parent,
                    position
                )
            }

        val computerResultList = gameState!!.computerResultList

        val computerResultListAdapter = ArrayAdapter<Result>(this, R.layout.list_item)

        computerResultList.observe(
            this
        ) { list: ArrayList<Result>? ->
            computerResultListAdapter.clear()
            computerResultListAdapter.addAll(list!!)
            computerResultListAdapter.notifyDataSetChanged()
            updateScore()
        }

        computerResultListView?.setAdapter(computerResultListAdapter)

        computerResultListView?.setOnItemClickListener { parent: AdapterView<*>, _: View?, position: Int, _: Long ->
            wordDefinitionLookup(
                parent,
                position
            )
        }

        gameState!!.countDownTimerCurrentValue.observe(
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

        addGestureHandler(findViewById(R.id.letterGridView))

        labelDices()

        updateDiceState(-1)
        updateOkButton()

        updateScore()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGestureHandler(tableLayout: TableLayout) {
        // Iterate through all buttons in the TableLayout

        for (i in 0 until tableLayout.childCount) {
            val child = tableLayout.getChildAt(i)
            if (child is TableRow) {
                for (j in 0 until child.childCount) {
                    val view = child.getChildAt(j)
                    if (view is Button) {
                        // Attach the OnTouchListener to each button
                        view.setOnClickListener { letterClick(view) }

                        view.setOnTouchListener(object : OnTouchListener {
                            private var firstButtonPressed: Button? = null
                            private var lastButtonPressed: Button? = null

                            override fun onTouch(v: View, event: MotionEvent): Boolean {
                                val action = event.action
                                var x = event.x.toInt()
                                var y = event.y.toInt()

                                // Convert the touch coordinates to screen coordinates
                                val location = IntArray(2)
                                v.getLocationOnScreen(location)
                                x += location[0]
                                y += location[1]

                                // Find the button at the current touch position
                                val button = findButtonAtPosition(tableLayout, x, y, action)

                                if (button != null) {
                                    when (action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            firstButtonPressed = button
                                            if (button !== lastButtonPressed) {
                                                if (button.hasOnClickListeners()) {
                                                    button.callOnClick()
                                                }
                                                lastButtonPressed = button
                                            }
                                        }

                                        MotionEvent.ACTION_MOVE -> if (button !== lastButtonPressed) {
                                            if (button.hasOnClickListeners()) {
                                                button.callOnClick()
                                            }
                                            lastButtonPressed = button
                                        }

                                        MotionEvent.ACTION_UP -> {
                                            Log.d(TAG, "Button up")
                                            if (firstButtonPressed !== lastButtonPressed) okClick()
                                            lastButtonPressed = null
                                        }
                                    }
                                }

                                return true
                            }
                        })
                    }
                }
            }
        }
    }

    // Helper method to find the button at a specific position
    private fun findButtonAtPosition(
        tableLayout: TableLayout,
        x: Int,
        y: Int,
        action: Int
    ): Button? {
        var touchAreaPercent = 0f

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> touchAreaPercent = 1f
            MotionEvent.ACTION_MOVE -> touchAreaPercent = 0.6f
        }

        for (i in 0 until tableLayout.childCount) {
            val child = tableLayout.getChildAt(i)
            if (child is TableRow) {
                for (j in 0 until child.childCount) {
                    val view = child.getChildAt(j)
                    if (view is Button) {
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        val buttonLeft = location[0]
                        val buttonTop = location[1]

                        val buttonWidth = view.width
                        val buttonHeight = view.height

                        val buttonRight = buttonLeft + buttonWidth
                        val buttonBottom = buttonTop + buttonHeight

                        val deltaWidth = buttonWidth * (1 - touchAreaPercent) / 2
                        val deltaHeight = buttonHeight * (1 - touchAreaPercent) / 2

                        // Check if the touch position is within the button's bounds
                        if (x >= buttonLeft + deltaWidth && x <= buttonRight - deltaWidth && y >= buttonTop + deltaHeight && y <= buttonBottom - deltaHeight) {
                            return view
                        }
                    }
                }
            }
        }
        return null
    }

    private fun wordDefinitionLookup(parent: AdapterView<*>, position: Int) {
        val selectedItem = parent.getItemAtPosition(position) as Result

        val selectedWord = selectedItem.result

        val lookupService = getWordDefinitionLookupService(gameState!!.dictionaryName!!)

        if (lookupService == null) {
            Toast.makeText(
                this,
                R.string.word_definition_lookup_not_supported_for_this_dictionary,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val wordInfo = gameState!!.getWordInfoFromCache(
                selectedWord,
                lookupService.language
            )

            if (wordInfo != null) {
                val wordDefinition = wordInfo.wordDefinition
                if (wordDefinition.isNullOrBlank()) {
                    displayToast("Definition not found for: $selectedWord", Toast.LENGTH_SHORT)
                } else {
                    displayWordDefinition(wordDefinition)
                }
            } else {
                if (isNetworkAvailable(applicationContext)) {
                    Toast.makeText(
                        this,
                        "Looking up definition for $selectedItem", Toast.LENGTH_SHORT
                    ).show()
                    lookupService.lookupWordDefinition(gameState!!, selectedWord)
                } else {
                    Toast.makeText(
                        this,
                        R.string.no_internet_connection_available,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getWordDefinitionLookupService(dictionaryName: String): WordDefinitionLookupService? {
        return when (dictionaryName.lowercase(Locale.getDefault())) {
            "2of4brinf", "2of12inf" -> EnglishWordDefinitionLookupService()
            "german" -> GermanWordDefinitionLookupService()
            else -> null
        }
    }

    public override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (!gameState!!.hasGameStarted()) {
            if (gameState!!.countDownTime >= 0) showConfirmStartGameDialog(true)
            else {
                shuffle()
            }
        } else {
            if (gameState!!.isTimeUp) {
                showTimeIsUpDialog()
            }
        }

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (preferencesChanged) {
            countDownView!!.visibility = View.INVISIBLE
            prefs
            shuffle()
            preferencesChanged = false
        }
    }

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager
            .getDefaultSharedPreferences(baseContext)

    public override fun onPause() {
        super.onPause()
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var preferencesChanged = false

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Log.i(TAG, "Preferences changed for $key")
        preferencesChanged = true
    }

    private val prefs: Unit
        get() {
            val prefs = sharedPreferences

            val defaultDict = getString(R.string.default_dict)
            gameState!!.dictionaryName = prefs.getString("dict_pref", defaultDict)
            gameState!!.setScoringAlgorithm(prefs.getString("scoring_pref", "count"))
            gameState!!.isAllow3LetterWords = prefs
                .getBoolean("threeLetterPref", true)

            gameState!!.setAutoAddPrefixalWords(
                prefs
                    .getBoolean("autoAddPrefixPref", true)
            )

            if (prefs.getBoolean("countdown_pref", false)) {
                val timeStr = prefs.getString("countdown_time_pref", "02:00")!!
                val time = parseTime(timeStr)
                gameState!!.countDownTime = time
            } else {
                gameState!!.countDownTime = -1
            }
        }

    private fun showTimeIsUpDialog() {
        if (isFinishing) return

        val builder = AlertDialog.Builder(this@WordFinder)
        builder.setMessage(R.string.time_up_dialog_msg)
            .setTitle(R.string.time_up_dialog_title)
            .setPositiveButton(
                R.string.time_up_dialog_ok
            ) { _: DialogInterface?, _: Int -> gameState!!.isTimeUp = false }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun showConfirmShuffleDialog() {
        val builder = AlertDialog.Builder(this@WordFinder)
        builder.setMessage(R.string.shuffle_confirm_msg)
            .setTitle(R.string.shuffle_confirm_title)
            .setPositiveButton(
                R.string.shuffle_ok_text
            ) { _: DialogInterface?, _: Int -> shuffle() }
            .setNegativeButton(
                R.string.shuffle_cancle_text
            ) { _: DialogInterface?, _: Int -> }

        val dialog = builder.create()
        dialog.show()
    }

    private fun showConfirmStartGameDialog(doShuffle: Boolean) {
        val builder = AlertDialog.Builder(this@WordFinder)
        builder.setMessage(R.string.start_game_diag_msg)
            .setTitle(R.string.start_game_diag_title)
            .setPositiveButton(
                R.string.start_game_diag_ok
            ) { _: DialogInterface?, _: Int ->
                if (doShuffle) shuffle()
            }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
    }

    @SuppressLint("SetTextI18n")
    fun updateTimeView(time: Long) {
        if (isFinishing) return

        if (time >= 0) {
            if (countDownView!!.visibility != View.VISIBLE) countDownView!!.visibility =
                View.VISIBLE
            val h = time / 60
            val m = time % 60
            var ms = m.toString()
            if (ms.length == 1) ms = "0$ms"
            countDownView!!.text = "$h:$ms"
            if (time == 0L) {
                showTimeIsUpDialog()
            }
        } else {
            countDownView!!.visibility = View.INVISIBLE
        }
    }

    private fun parseTime(timeStr: String): Long {
        if (timeStr.contains(":")) {
            val c = timeStr.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return 1000 * (c[0].toInt() * 60L + c[1].toInt())
        } else {
            return timeStr.toInt() * 1000L
        }
    }

    override fun onStart() {
        super.onStart()
        prefs
    }

    private fun updateDiceState(move: Int) {
        if (move >= 0) {
            for (button in letterButtons) {
                button?.isEnabled = false
                button?.setContentDescription("Unavailable Letter Button")
            }

            for (bid in MOVES[move]) {
                val enabled = gameState!!.isAvailable(bid)
                letterButtons[bid]!!.isEnabled = enabled
                if (!enabled) {
                    letterButtons[bid]!!.setContentDescription(
                        "Disabled Letter " + gameState!!.getBoard(
                            bid
                        )
                    )
                }
            }
        } else {
            for (c in 0..15) {
                val l = gameState!!.getBoard(c)
                val enabled = l != '\u0000' && gameState!!.isAvailable(c)
                letterButtons[c]!!.isEnabled = enabled
                if (!enabled) {
                    letterButtons[c]!!.setContentDescription("Disabled Letter $l")
                }
            }
        }
    }

    private fun labelDices() {
        for (c in 0..15) {
            val l = gameState!!.getBoard(c)
            letterButtons[c]!!.setText((if (l == 'Q') "Qu" else l).toString())
            letterButtons[c]!!.setContentDescription("Letter $l")
        }
    }

    private fun shuffle() {
        showComputerResults(false)

        gameState!!.stopSolving()

        playerResultList!!.clear()

        gameState!!.shuffle()

        labelDices()

        gameState!!.startSolving()

        updateDiceState(gameState!!.lastMove)
        updateOkButton()
        gameState!!.startCountDown()
        updateScore()
    }

    private fun shuffleClick() {
        showConfirmShuffleDialog()
    }

    private val letterButtons = arrayOfNulls<LetterButton>(16)

    private val idToLetterButton = SparseArray<LetterButton?>()

    private fun letterClick(view: View) {
        val pressedButton = checkNotNull(idToLetterButton[view.id])

        if (!pressedButton.isEnabled) return

        val move = pressedButton.pos
        gameState!!.play(move)

        updateOkButton()
        updateDiceState(move)
    }

    fun okClick() {
        var guess = gameState!!.currentGuess

        if (gameState!!.validatePlayerGuess(guess) == null) {
            playerResultList!!.insert(Result(guess), 0)
            if (gameState!!.autoAddPrefixalWords()) {
                testAndAddPrefixWords(guess)
            }
        } else {
            guess = guess.replace("Q".toRegex(), "QU")
            val validationResult = gameState!!.validatePlayerGuess(guess)
            if (validationResult == null) {
                playerResultList!!.insert(Result(guess), 0)
                if (gameState!!.autoAddPrefixalWords()) {
                    testAndAddPrefixWords(guess)
                }
            } else {
                val text: String = when (validationResult) {
                    PlayerGuessState.ALREADY_FOUND -> getString(R.string.WordAlreadyFound)
                    PlayerGuessState.NOT_IN_DICTIONARY -> getString(R.string.WordNotInDictionary)
                    PlayerGuessState.TOO_SHORT -> getString(R.string.WordGuessTooShort)
                }

                val context = applicationContext
                val toast = Toast.makeText(context, "\"$guess\" $text", Toast.LENGTH_SHORT)
                toast.show()

                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(100)
            }
        }

        gameState!!.clearGuess()
        updateScore()
        updateDiceState(-1)
        updateOkButton()
    }

    private fun testAndAddPrefixWords(word: String) {
        var localWord = word
        while (localWord.isNotEmpty()) {
            localWord = localWord.substring(0, localWord.length - 1) // Remove the last character
            val result = gameState!!.validatePlayerGuess(localWord)
            if (result == null) {
                playerResultList!!.insert(Result(localWord), 0)
            } else {
                when (result) {
                    PlayerGuessState.ALREADY_FOUND, PlayerGuessState.NOT_IN_DICTIONARY -> continue
                    PlayerGuessState.TOO_SHORT -> return
                }
            }
        }
    }

    private fun updateOkButton() {
        val currentGuess = gameState!!.currentGuess.replace("Q".toRegex(), "Q(u)")
        okButton!!.text = currentGuess
        if (currentGuess.isEmpty()) {
            okButton!!.visibility = View.INVISIBLE
        } else {
            okButton!!.visibility = View.VISIBLE
            okButton!!.contentDescription =
                "Current guess: " + (gameState!!.currentGuess.ifBlank { "empty" })
            val minLength = if (gameState!!.isAllow3LetterWords) 3 else 4
            val enabled = gameState!!.currentGuess.length >= minLength
            if (enabled) {
                okButton!!.setTextColor(guessButtonEnabledTextColour)
            } else {
                okButton!!.setTextColor(Color.parseColor("#FAC6C6"))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateScore() {
        if (scoreTextView != null) scoreTextView!!.text =
            (gameState!!.playerScore.toString() + " / "
                    + gameState!!.computerScore)
    }

    private fun showComputerResults(show: Boolean) {
        this.showComputerResultsFlag = show

        if (show) {
            showAllRow!!.visibility = View.GONE
            computerResultListView!!.visibility = View.VISIBLE
        } else {
            showAllRow!!.visibility = View.VISIBLE
            computerResultListView!!.visibility = View.INVISIBLE
        }
    }

    private fun solveClick() {
        showComputerResults(true)
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
                showInfo(fragmentManager)
                return true
            }
            R.id.menu_item_prefs -> {
                showPreferences()
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

    private fun displayToast(text: String?, length: Int) {
        runOnUiThread { Toast.makeText(this, text, length).show() }
    }

    private fun displayWordDefinition(definitionStr: String) {
        runOnUiThread {
            val view = findViewById<View>(android.R.id.content)
            val snackbar = Snackbar.make(view, definitionStr, Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view

            val textView =
                snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            if (textView != null) {
                textView.maxLines = 10
            } else {
                Log.e(
                    "Util",
                    "TextView not found in Snackbar view to adjust number of lines"
                )
            }

            val params = snackbarView.layoutParams
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT // Wrap the width to text size
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Optional: Wrap height
            snackbarView.layoutParams = params

            val layoutParams = snackbarView.layoutParams as FrameLayout.LayoutParams
            layoutParams.gravity = Gravity.CENTER // Adjust gravity if needed
            snackbarView.layoutParams = layoutParams
            snackbar.show()
        }
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