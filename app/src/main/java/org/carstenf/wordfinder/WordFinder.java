/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;

public class WordFinder extends AppCompatActivity implements OnSharedPreferenceChangeListener {

	public final static String TAG = "CF_WF";

	final static int[][] MOVES = { { 1, 4, 5 }, { 0, 2, 4, 5, 6 },
			{ 1, 3, 5, 6, 7 }, { 2, 6, 7 }, {

			0, 1, 5, 8, 9 }, { 0, 1, 2, 4, 6, 8, 9, 10 },
			{ 1, 2, 3, 5, 7, 9, 10, 11 }, { 2, 3, 6, 10, 11 }, {

			4, 5, 9, 12, 13 }, { 4, 5, 6, 8, 10, 12, 13, 14 },
			{ 5, 6, 7, 9, 11, 13, 14, 15 }, { 6, 7, 10, 14, 15 }, {

			8, 9, 13 }, { 8, 9, 10, 12, 14 }, { 9, 10, 11, 13, 15 },
			{ 10, 11, 14 } };
	private static final String SHOW_COMPUTER_RESULTS_FLAG = "SHOW_COMPUTER_RESULTS_FLAG";

	private ArrayAdapter<Result> playerResultList;

	private ListView computerResultListView;


	private Button okButton;

	private GameState gameState = null;

	private View showAllRow;

	private TextView scoreTextView;

	private TextView countDownView;
    private int guessButtonEnabledTextColour;
	private boolean showComputerResultsFlag;

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
		outState.putBoolean(SHOW_COMPUTER_RESULTS_FLAG, showComputerResultsFlag);
    }

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		showComputerResults(savedInstanceState.getBoolean(SHOW_COMPUTER_RESULTS_FLAG, false));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.okButton = findViewById(R.id.okButton);

		this.showAllRow = findViewById(R.id.showAllRow);

		ListView playerResultListView = findViewById(R.id.playerResultsList);
		this.computerResultListView = findViewById(R.id.computerResultsList);

		this.countDownView = findViewById(R.id.chronometer1);
		this.countDownView.setVisibility(INVISIBLE);

		scoreTextView = findViewById(R.id.scoreTextView);
		for (int c = 0; c < 16; c++) {
			Button button = this.findViewById(letterButtonIds[c]);
			letterButtons[c] = new LetterButton(c, button);
			idToLetterButton.put(letterButtonIds[c], letterButtons[c]);
			button.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){

				@Override
				public void onGlobalLayout() {
					button.getViewTreeObserver().removeOnGlobalLayoutListener(this);

					// Calculate text size based on button height
					int buttonHeight = button.getHeight();
					float textSize = buttonHeight * 0.2f;
					button.setTextSize(textSize);
				}
			});
		}

		gameState = new ViewModelProvider(this).get(GameState.class);
		try {
            gameState.setDictionary(new Dictionary(this));
        } catch (IOException e) {
			throw new RuntimeException("Could not create Dictionaries: "+e.getMessage(), e);
        }

		gameState.getWordLookupError().observe(this, (String text) -> {
			if(text!=null) displayToast(text, Toast.LENGTH_SHORT);
		});

		gameState.getWordLookupResult().setValue(null);
		gameState.getWordLookupResult().observe(this, (WordInfo wordInfo) -> {
			if(wordInfo==null) return;

			if(wordInfo.getWordDefinition() == null && wordInfo.getWordDefinition().isBlank()) {
				displayToast("Definition not found for: "+ wordInfo.getWord(), Toast.LENGTH_SHORT);
			} else {
				displayWordDefinition(wordInfo.getWordDefinition());
			}
		});

		playerResultList = new ArrayAdapter<>(this, R.layout.list_item,
				gameState.getPlayerResultList());
		playerResultListView.setAdapter(playerResultList);

		playerResultListView.setOnItemClickListener((parent, view, position, id) -> wordDefinitionLookup(parent, position));

		MutableLiveData<ArrayList<Result>> computerResultList = gameState.getComputerResultList();

		ArrayAdapter<Result>  computerResultListAdapter = new ArrayAdapter<>(this, R.layout.list_item);

		computerResultList.observe(this, list -> {
			computerResultListAdapter.clear();
			computerResultListAdapter.addAll(list);
			computerResultListAdapter.notifyDataSetChanged();
			updateScore();
		});

		computerResultListView.setAdapter(computerResultListAdapter);

		computerResultListView.setOnItemClickListener((parent, view, position, id) -> wordDefinitionLookup(parent, position));

		gameState.getCountDownTimerCurrentValue().observe(this, this::updateTimeView);

		TypedArray themeArray = getTheme().obtainStyledAttributes(new int[] {android.R.attr.editTextColor});
		try {
			int index = 0;
			int defaultColourValue = 0;
			guessButtonEnabledTextColour = themeArray.getColor(index, defaultColourValue);
		}
		finally
		{
			// Calling recycle() is important. Especially if you use a lot of TypedArrays
			// http://stackoverflow.com/a/13805641/8524
			themeArray.recycle();
		}

		addGestureHandler(findViewById(R.id.letterGridView));

		labelDices();

		updateDiceState(-1);
		updateOkButton();

		updateScore();
	}

	private void addGestureHandler(TableLayout tableLayout) {
		// Iterate through all buttons in the TableLayout

		for (int i = 0; i < tableLayout.getChildCount(); i++) {
			View child = tableLayout.getChildAt(i);
			if (child instanceof TableRow) {
				TableRow row = (TableRow) child;
				for (int j = 0; j < row.getChildCount(); j++) {
					View view = row.getChildAt(j);
					if (view instanceof Button) {
						Button button = (Button) view;

						// Attach the OnTouchListener to each button
						button.setOnTouchListener(new View.OnTouchListener() {
							private Button firstButtonPressed;
							private Button lastButtonPressed = null;

							@Override
							public boolean onTouch(View v, MotionEvent event) {
								int action = event.getAction();
								int x = (int) event.getX();
								int y = (int) event.getY();

								// Convert the touch coordinates to screen coordinates
								int[] location = new int[2];
								v.getLocationOnScreen(location);
								x += location[0];
								y += location[1];

								// Find the button at the current touch position
								Button button = findButtonAtPosition(tableLayout, x, y, action);

								if (button != null) {
									switch (action) {
										case MotionEvent.ACTION_DOWN:
											firstButtonPressed = button;
										case MotionEvent.ACTION_MOVE:
											if (button != lastButtonPressed) {
												if (button.hasOnClickListeners()) {
													button.callOnClick();
												}
												lastButtonPressed = button;
											}
											break;
										case MotionEvent.ACTION_UP:
											Log.d(TAG, "Button up");
											if(firstButtonPressed != lastButtonPressed)
												okClick(null);
											lastButtonPressed = null;
											break;
									}
								}

								return true;
							}
						});
					}
				}
			}
		}
	}

	// Helper method to find the button at a specific position
	private Button findButtonAtPosition(TableLayout tableLayout, int x, int y, int action) {
		float touchAreaPercent = 0;

		switch (action) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				touchAreaPercent = 1;
				break;

			case MotionEvent.ACTION_MOVE:
				touchAreaPercent = 0.6f;
				break;
		}

		for (int i = 0; i < tableLayout.getChildCount(); i++) {
			View child = tableLayout.getChildAt(i);
			if (child instanceof TableRow) {
				TableRow row = (TableRow) child;
				for (int j = 0; j < row.getChildCount(); j++) {
					View view = row.getChildAt(j);
					if (view instanceof Button) {
						Button button = (Button) view;
						int[] location = new int[2];
						button.getLocationOnScreen(location);
						int buttonLeft = location[0];
						int buttonTop = location[1];

						int buttonWidth = button.getWidth();
						int buttonHeight = button.getHeight();

                        int buttonRight = buttonLeft + buttonWidth;
						int buttonBottom = buttonTop + buttonHeight;

						float deltaWidth = buttonWidth * (1 - touchAreaPercent)/2;
						float deltaHeight = buttonHeight * (1 - touchAreaPercent)/2;

						// Check if the touch position is within the button's bounds
						if (x >= buttonLeft+deltaWidth && x <= buttonRight-deltaWidth &&
								y >= buttonTop+deltaHeight && y <= buttonBottom- deltaHeight) {
							return button;
						}
					}
				}
			}
		}
		return null;
	}
	private void wordDefinitionLookup(AdapterView<?> parent, int position) {
		Result selectedItem = (Result) parent.getItemAtPosition(position);

		if(selectedItem==null) return;
		String selectedWord = selectedItem.getResult();

		WordDefinitionLookupService lookupService = getWordDefinitionLookupService(gameState.getDictionaryName());

		if (lookupService == null) {
			Toast.makeText(this, R.string.word_definition_lookup_not_supported_for_this_dictionary, Toast.LENGTH_SHORT).show();
		} else {
			WordInfo wordInfo = gameState.getWordInfoFromCache(selectedWord, lookupService.getLanguage());

			if (wordInfo != null) {
				String wordDefinition = wordInfo.getWordDefinition();
				if(wordDefinition ==null || wordDefinition.isBlank()) {
					displayToast("Definition not found for: "+ selectedWord, Toast.LENGTH_SHORT);
				} else {
					displayWordDefinition(wordDefinition);
				}
			} else {
				if (Util.isNetworkAvailable(getApplicationContext())) {
					Toast.makeText(this, "Looking up definition for " + selectedItem, Toast.LENGTH_SHORT).show();
					lookupService.lookupWordDefinition(gameState, selectedWord);
				} else {
					Toast.makeText(this, R.string.no_internet_connection_available, Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	private WordDefinitionLookupService getWordDefinitionLookupService(String dictionaryName) {
		switch (dictionaryName.toLowerCase()){
			case "2of4brinf":
			case "2of12inf":
				return new EnglishWordDefinitionLookupService();
			case "german":
				return new GermanWordDefinitionLookupService();
			default:
				return null;
		}
	}

	@Override
    public void onResume() {
        super.onResume();

		if (!gameState.hasGameStarted()) {
			if(gameState.getCountDownTime()>=0)
				showConfirmStartGameDialog(true);
			else {
				shuffle();
			}
		} else  {
			if(gameState.isTimeUp()) {
				showTimeIsUpDialog();
			}
		}

		getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        if(preferencesChanged){
            this.countDownView.setVisibility(INVISIBLE);
            getPrefs();
            shuffle();
            preferencesChanged = false;
        }
    }

    private SharedPreferences getSharedPreferences() {
		return PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    private boolean preferencesChanged = false;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    Log.i(TAG, "Preferences changed for "+key);
        preferencesChanged  = true;
    }

	private void getPrefs() {
        SharedPreferences prefs = getSharedPreferences();

		String defaultDict = getString(R.string.default_dict);
		gameState.setDictionaryName(prefs.getString("dict_pref", defaultDict));
		gameState.setScoringAlgorithm(prefs.getString("scoring_pref", "count"));
		gameState.setAllow3LetterWords(prefs
				.getBoolean("threeLetterPref", true));

		gameState.setAutoAddPrefixalWords(prefs
				.getBoolean("autoAddPrefixPref", true));

		if (prefs.getBoolean("countdown_pref", false)) {
			String timeStr = prefs.getString("countdown_time_pref", "02:00");
			long time = parseTime(timeStr);
			gameState.setCountDownTime(time);

		} else {
			gameState.setCountDownTime(-1);
		}
	}

	public void showTimeIsUpDialog() {
        if(isFinishing()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(WordFinder.this);
        builder.setMessage(R.string.time_up_dialog_msg)
                .setTitle(R.string.time_up_dialog_title)
                .setPositiveButton(R.string.time_up_dialog_ok, (dialog, which) -> gameState.setTimeUp(false));

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void showConfirmShuffleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(WordFinder.this);
        builder.setMessage(R.string.shuffle_confirm_msg)
                .setTitle(R.string.shuffle_confirm_title)
                .setPositiveButton(R.string.shuffle_ok_text, (dialog, which) -> shuffle())
                .setNegativeButton(R.string.shuffle_cancle_text, (dialog, which) -> {
				});

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showConfirmStartGameDialog(boolean doShuffle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(WordFinder.this);
        builder.setMessage(R.string.start_game_diag_msg)
                .setTitle(R.string.start_game_diag_title)
                .setPositiveButton(R.string.start_game_diag_ok, (dialog, which) -> {
					if(doShuffle) shuffle();
				});

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

	@SuppressLint("SetTextI18n")
	void updateTimeView(long time) {
		if(isFinishing()) return;

		if (time >= 0) {
			if (this.countDownView.getVisibility() != VISIBLE)
				this.countDownView.setVisibility(VISIBLE);
			long h = time / 60;
			long m = time % 60;
			String ms = String.valueOf(m);
			if (ms.length() == 1)
				ms = "0" + ms;
			countDownView.setText(h + ":" + ms);
			if(time== 0) {
				showTimeIsUpDialog();
			}
		} else {
			this.countDownView.setVisibility(INVISIBLE);
		}
	}

	private long parseTime(String timeStr) {
		if (timeStr.contains(":")) {
			String[] c = timeStr.split(":");
			return 1000 * (Integer.parseInt(c[0]) * 60L + Integer.parseInt(c[1]));
		} else {
			return Integer.parseInt(timeStr) * 1000L;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		getPrefs();
	}

	private void updateDiceState(int move) {
		if (move >= 0) {
			for (LetterButton button : letterButtons) {
				button.setEnabled(false);
				button.setContentDescription("Unavailable Letter Button");
			}

			for (int bid : MOVES[move]) {
				boolean enabled = gameState.isAvailable(bid);
				letterButtons[bid].setEnabled(enabled);
				if(!enabled) {
					letterButtons[bid].setContentDescription("Disabled Letter "+ gameState.getBoard(bid));
				}
			}
		} else {
			for (int c = 0; c < 16; c++) {
				char l = gameState.getBoard(c);
				boolean enabled = l != '\0' && gameState.isAvailable(c);
				letterButtons[c].setEnabled(enabled);
				if(!enabled) {
					letterButtons[c].setContentDescription("Disabled Letter "+ l);
				}
			}
		}
	}

	private void labelDices() {
		for (int c = 0; c < 16; c++) {
			char l = gameState.getBoard(c);
			letterButtons[c].setText(String.valueOf(l == 'Q' ? "Qu" : l));
			letterButtons[c].setContentDescription("Letter "+ l);
		}
	}

	public void shuffle() {
        showComputerResults(false);

        gameState.stopSolving();

        playerResultList.clear();

        gameState.shuffle();

        labelDices();

        gameState.startSolving();

        updateDiceState(gameState.getLastMove());
        updateOkButton();
        gameState.startCountDown();
        updateScore();
    }

	public void shuffleClick(View view) {
        showConfirmShuffleDialog();
	}

    final private LetterButton[] letterButtons = new LetterButton[16];

    final private SparseArray<LetterButton> idToLetterButton = new SparseArray<>();

	private final static int[] letterButtonIds = { R.id.button01, R.id.button02,
			R.id.button03, R.id.button04, R.id.button11, R.id.button12,
			R.id.button13, R.id.button14, R.id.button21, R.id.button22,
			R.id.button23, R.id.button24, R.id.button31, R.id.button32,
			R.id.button33, R.id.button34 };

	public void letterClick(@NonNull View view) {
		LetterButton pressedButton = idToLetterButton.get(view.getId());

        assert pressedButton != null;
		if(! pressedButton.isEnabled()) return;

        int move = pressedButton.getPos();
		gameState.play(move);

		updateOkButton();
		updateDiceState(move);
	}

	public void okClick(View view) {
		String guess = gameState.getCurrentGuess();

		if (gameState.validatePlayerGuess(guess) == null) {
			playerResultList.insert(new Result(guess), 0);
			if(gameState.autoAddPrefixalWords()) {
				testAndAddPrefixWords(guess);
			}
		} else {
			guess = guess.replaceAll("Q", "QU");
			GameState.PLAYER_GUESS_STATE validationResult = gameState.validatePlayerGuess(guess);
			if (validationResult ==null) {
				playerResultList.insert(new Result(guess), 0);
				if(gameState.autoAddPrefixalWords()) {
					testAndAddPrefixWords(guess);
				}
			} else {
				String text = "";
				switch (validationResult){
					case ALREADY_FOUND:
						text = getString(R.string.WordAlreadyFound);
						break;
					case NOT_IN_DICTIONARY:
						text = getString(R.string.WordNotInDictionary);
						break;
					case TOO_SHORT:
						text = getString(R.string.WordGuessTooShort);
				}

                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, "\""+guess+"\" " + text, Toast.LENGTH_SHORT);
                toast.show();

				Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(100);
			}
		}

		gameState.clearGuess();
		updateScore();
		updateDiceState(-1);
		updateOkButton();
	}

	private void testAndAddPrefixWords(@NonNull String word) {
		while (!word.isEmpty()) {
			word = word.substring(0, word.length() - 1); // Remove the last character
			GameState.PLAYER_GUESS_STATE result = gameState.validatePlayerGuess(word);
			if(result==null) {
				playerResultList.insert(new Result(word), 0);
			} else {
				switch (result) {
					case ALREADY_FOUND:
					case NOT_IN_DICTIONARY:
						continue;
					case TOO_SHORT:
						return;
				}
			}
		}

	}

	private void updateOkButton() {
		String currentGuess = gameState.getCurrentGuess().replaceAll("Q", "Q(u)");
		okButton.setText(currentGuess);
		if(currentGuess.isEmpty()) {
			okButton.setVisibility(INVISIBLE);
		} else {
			okButton.setVisibility(VISIBLE);
			okButton.setContentDescription("Current guess: " + (gameState.getCurrentGuess().isBlank() ? "empty" : gameState.getCurrentGuess()));
			int minLength = gameState.isAllow3LetterWords() ? 3 : 4;
			boolean enabled = gameState.getCurrentGuess().length() >= minLength;
			if (enabled) {
				okButton.setTextColor(guessButtonEnabledTextColour);
			} else {
				okButton.setTextColor(Color.parseColor("#FFA0A0"));
			}
		}
	}

	@SuppressLint("SetTextI18n")
    private void updateScore() {
		if (scoreTextView != null)
			scoreTextView.setText(gameState.getPlayerScore() + " / "
					+ gameState.getComputerScore());
	}

	private void showComputerResults(boolean show) {
		this.showComputerResultsFlag = show;

		if (show) {
			showAllRow.setVisibility(GONE);
			computerResultListView.setVisibility(VISIBLE);
		} else {
			showAllRow.setVisibility(VISIBLE);
			computerResultListView.setVisibility(INVISIBLE);
		}
	}

	public void solveClick(View view) {
		showComputerResults(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.menu_item_info) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			InfoDialogFragment.Companion.showInfo(fragmentManager);
			return true;
		} else if (item.getItemId() == R.id.menu_item_prefs) {
			showPreferences();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void showPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(),
				WordFinderPreferences.class);
		startActivity(settingsActivity);
	}

	public void displayToast(String text, int length) {
		runOnUiThread(() -> Toast.makeText(this, text, length).show());
	}

	public void displayWordDefinition(String definitionStr) {
		runOnUiThread(() -> {
			View view = findViewById(android.R.id.content);
			Snackbar snackbar = Snackbar.make(view, definitionStr, Snackbar.LENGTH_LONG);
			View snackbarView = snackbar.getView();

			TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
			if (textView != null) {
				textView.setMaxLines(10);
			} else {
				Log.e("Util", "TextView not found in Snackbar view to adjust number of lines");
			}

			ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
			params.width = ViewGroup.LayoutParams.WRAP_CONTENT; // Wrap the width to text size
			params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Optional: Wrap height
			snackbarView.setLayoutParams(params);

			FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
			layoutParams.gravity = Gravity.CENTER; // Adjust gravity if needed
			snackbarView.setLayoutParams(layoutParams);

			snackbar.show();
		});
	}
}