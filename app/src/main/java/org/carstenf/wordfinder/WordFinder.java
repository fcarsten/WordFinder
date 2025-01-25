/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

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

	private ArrayAdapter<Result> playerResultList;

	private ListView computerResultListView;

	private ArrayAdapter<Result> computerResultList;

	private Button okButton;

	private GameState gameState = null;

	private View showAllRow;

	private TextView scoreTextView;

	private TextView countDownView;
    private int guessButtonEnabledTextColour;

    /** Called when the activity is first created. */

	public Activity getActivity() {
		return this;
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
		this.countDownView.setVisibility(View.INVISIBLE);

		scoreTextView = findViewById(R.id.scoreTextView);
		for (int c = 0; c < 16; c++) {
			letterButtons[c] = new LetterButton(c, this.findViewById(letterButtonIds[c]));
			idToLetterButton.put(letterButtonIds[c], letterButtons[c]);
		}


		gameState = (GameState) getLastCustomNonConfigurationInstance();
		if (gameState == null) {
			try {
				gameState = new GameState(this, new Dictionary(this));
			} catch (IOException e) {
				throw new RuntimeException("Could not create Dictionaries: "+e.getMessage(), e);
			}
		} else {
			gameState.setOwner(this);
		}

		playerResultList = new ArrayAdapter<>(this, R.layout.list_item,
				gameState.getPlayerResultList());
		playerResultListView.setAdapter(playerResultList);

		playerResultListView.setOnItemClickListener((parent, view, position, id) -> {
            Result selectedItem = (Result) parent.getItemAtPosition(position);

            if(selectedItem!=null ) {
				if(! definitionSupported(gameState.getDictionaryName())) {
					Toast.makeText(this, R.string.word_definition_lookup_not_supported_for_this_dictionary, Toast.LENGTH_SHORT).show();
				} else {
					if (Util.isNetworkAvailable(getApplicationContext())) {
						Util.lookupWordDefinition(getActivity(), getApplicationContext(), selectedItem.toString());
					} else {
						Toast.makeText(this, R.string.no_internet_connection_available, Toast.LENGTH_SHORT).show();
					}
				}
            }
        });

		computerResultList = new ArrayAdapter<>(this, R.layout.list_item,
				gameState.getComputerResultList());
		computerResultListView.setAdapter(computerResultList);

		computerResultListView.setOnItemClickListener((parent, view, position, id) -> {
            Result selectedItem = (Result) parent.getItemAtPosition(position);

            if(selectedItem!=null) {
				if(! definitionSupported(gameState.getDictionaryName())) {
					Toast.makeText(this, R.string.word_definition_lookup_not_supported_for_this_dictionary, Toast.LENGTH_SHORT).show();
				} else {
					if (Util.isNetworkAvailable(getApplicationContext())) {
						Util.lookupWordDefinition(getActivity(), getApplicationContext(), selectedItem.toString());
					} else {
						Toast.makeText(this, R.string.no_internet_connection_available, Toast.LENGTH_SHORT).show();
					}
				}
            }
        });

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

		labelDices();

		updateDiceState(-1);
		updateOkButton();

		updateScore();
	}

	private boolean definitionSupported(String dictionaryName) {
		return dictionaryName.equalsIgnoreCase("2of4brinf") ||
				dictionaryName.equalsIgnoreCase("2of12inf");
	}

	@Override
    public void onResume() {
        super.onResume();
        getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        if(preferencesChanged){
            this.countDownView.setVisibility(View.INVISIBLE);
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

    public void showConfirmStartGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(WordFinder.this);
        builder.setMessage(R.string.start_game_diag_msg)
                .setTitle(R.string.start_game_diag_title)
                .setPositiveButton(R.string.start_game_diag_ok, (dialog, which) -> shuffle());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

	@SuppressLint("SetTextI18n")
	void updateTimeView(long time) {
		if(isFinishing()) return;

		if (time >= 0) {
			if (this.countDownView.getVisibility() != View.VISIBLE)
				this.countDownView.setVisibility(View.VISIBLE);
			long h = time / 60;
			long m = time % 60;
			String ms = String.valueOf(m);
			if (ms.length() == 1)
				ms = "0" + ms;
			countDownView.setText(h + ":" + ms);
		} else {
			showTimeIsUpDialog();
			this.countDownView.setVisibility(View.INVISIBLE);
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
        if (!gameState.hasGameStarted()) {
            showAllRow.setVisibility(View.INVISIBLE);
            if(gameState.getCountDownTime()>=0)
                showConfirmStartGameDialog();
            else
                shuffle();
        } else  {
            if(gameState.isTimeUp()) {
                showTimeIsUpDialog();
            }
            showAllRow.setVisibility(View.VISIBLE);
        }
    }

	private void updateDiceState(int move) {
		if (move >= 0) {
			for (LetterButton button : letterButtons) {
				button.setEnabled(false);
			}

			for (int bid : MOVES[move]) {
				letterButtons[bid].setEnabled(gameState.isAvailable(bid));
			}
		} else {
			for (int c = 0; c < 16; c++) {
				char l = gameState.getBoard(c);
				letterButtons[c].setEnabled(l != '\0' && gameState.isAvailable(c));
				// Can be taken if re-load due to orientation change
			}
		}
	}

	@Nullable
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		gameState.setOwner(null);
		return gameState;
	}

	private void labelDices() {
		for (int c = 0; c < 16; c++) {
			char l = gameState.getBoard(c);
			letterButtons[c].setText(String.valueOf(l == 'Q' ? "Qu" : l));
		}
	}

	public void shuffle() {
        showComputerResults(false);

        gameState.stopSolving();

        playerResultList.clear();
        computerResultList.clear();

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
        int move = pressedButton.getPos();
		gameState.play(move);

		updateOkButton();
		updateDiceState(move);
	}

	public void okClick(View view) {
		String guess = gameState.getCurrentGuess();
		if (gameState.validatePlayerGuess(guess) == null) {
			playerResultList.insert(new Result(guess), 0);
		} else {
			guess = guess.replaceAll("Q", "QU");
            String validationResult = gameState.validatePlayerGuess(guess);
			if (validationResult ==null) {
				playerResultList.insert(new Result(guess), 0);
			} else {

                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, "\""+guess+"\" " + validationResult, duration);
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

	private void updateOkButton() {
		okButton.setText(gameState.getCurrentGuess().replaceAll("Q", "Q(u)"));
		int minLength = gameState.isAllow3LetterWords() ? 3 : 4;
		boolean enabled = gameState.getCurrentGuess().length() >= minLength;
		if(enabled) {
            okButton.setTextColor(guessButtonEnabledTextColour);
        } else {
            okButton.setTextColor(Color.parseColor("#FFA0A0"));
        }
	}

	@SuppressLint("SetTextI18n")
    private void updateScore() {
		if (scoreTextView != null)
			scoreTextView.setText(gameState.getPlayerScore() + " / "
					+ gameState.getComputerScore());
	}

	private void showComputerResults(boolean show) {
		if (show) {
			showAllRow.setVisibility(View.GONE);
			computerResultListView.setVisibility(View.VISIBLE);
		} else {
			showAllRow.setVisibility(View.VISIBLE);
			computerResultListView.setVisibility(View.INVISIBLE);
		}
	}

	public void solveClick(View view) {
		showComputerResults(true);
	}

	public void updateComputerResultView() {
		if (computerResultList != null)
			computerResultList.notifyDataSetChanged();
		updateScore();
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
			showInfo();
			return true;
		} else if (item.getItemId() == R.id.menu_item_prefs) {
			showPreferences();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private static final int DIALOG_INFO = 0;

	@Nullable
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_INFO) {
			return createInfoDialog();
		}
		return null;
	}

	private Dialog createInfoDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Info");
		builder.setPositiveButton(R.string.OK, (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        String infoText = getString(R.string.InfoText);

        Spanned markup = Html
                .fromHtml(infoText.replace("X.X", BuildConfig.VERSION_NAME));

		TextView textView = new TextView(this);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		textView.setText(markup);
		textView.setLinksClickable(true);

		dialog.setView(textView, getResources().getDimensionPixelSize(R.dimen.dialog_margin), 0, 0 , 0);

		return dialog;
	}

	private void showPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(),
				WordFinderPreferences.class);
		startActivity(settingsActivity);
	}

	private void showInfo() {
		showDialog(DIALOG_INFO);
	}
}