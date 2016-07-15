/**
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import java.io.IOException;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class WordFinder extends Activity {

	public final static String TAG = "CF_WF";

	final static int MOVES[][] = { { 1, 4, 5 }, { 0, 2, 4, 5, 6 },
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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.okButton = (Button) findViewById(R.id.okButton);

		this.showAllRow = findViewById(R.id.showAllRow);

		ListView playerResultListView = (ListView) findViewById(R.id.playerResultsList);
		this.computerResultListView = (ListView) findViewById(R.id.computerResultsList);

		this.countDownView = (TextView) findViewById(R.id.chronometer1);
		scoreTextView = (TextView) findViewById(R.id.scoreTextView);
		for (int c = 0; c < 16; c++) {
			letterButtons[c] = new LetterButton(letterButtonIds[c], c,
					(Button) this.findViewById(letterButtonIds[c]));
			idToLetterButton.put(letterButtonIds[c], letterButtons[c]);
		}

		gameState = (GameState) getLastNonConfigurationInstance();
		if (gameState == null) {
			try {
				gameState = new GameState(this, new Dictionary(this));
			} catch (IOException e) {
				throw new RuntimeException("Could not create Dictionaries: "+e.getMessage(), e);
			}
		} else {
			gameState.setOwner(this);
		}

		playerResultList = new ArrayAdapter<Result>(this, R.layout.list_item,
				gameState.getPlayerResultList());
		playerResultListView.setAdapter(playerResultList);

		computerResultList = new ArrayAdapter<Result>(this, R.layout.list_item,
				gameState.getComputerResultList());
		computerResultListView.setAdapter(computerResultList);

		labelDices();

		updateDiceState(-1);
		updateOkButton();

		updateScore();
		showAllRow.setVisibility(View.INVISIBLE);
	}

	private void getPrefs() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		gameState.setDictionaryName(prefs.getString("dict_pref", "2of4brinf"));
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

	@SuppressLint("SetTextI18n")
	void updateTimeView(long time) {
		if (time >= 0) {
			if (this.countDownView.getVisibility() != View.VISIBLE)
				this.countDownView.setVisibility(View.VISIBLE);
			long h = time / 60;
			long m = time % 60;
			String ms = "" + m;
			if (ms.length() == 1)
				ms = "0" + ms;
			countDownView.setText(h + ":" + ms);
		} else {
			this.countDownView.setVisibility(View.INVISIBLE);

		}
	}

	private long parseTime(String timeStr) {
		if (timeStr.contains(":")) {
			String[] c = timeStr.split(":");
			return 1000 * (Integer.parseInt(c[0]) * 60 + Integer.parseInt(c[1]));
		} else {
			return Integer.parseInt(timeStr) * 1000;
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
			}

			for (int bid : MOVES[move]) {
				letterButtons[bid].setEnabled(!gameState.isTaken(bid));
			}
		} else {
			for (int c = 0; c < 16; c++) {
				char l = gameState.getBoard(c);
				letterButtons[c].setEnabled(l != '\0' && !gameState.isTaken(c));
				// Can be taken if re-load due to orientation change
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		gameState.setOwner(null);
		return gameState;
	}

	private void labelDices() {
		for (int c = 0; c < 16; c++) {
			char l = gameState.getBoard(c);
			letterButtons[c].setText("" + (l == 'Q' ? "Qu" : l));
		}
	}

	public void shuffleClick(View view) {
		showComputerResults(false);

		gameState.stopSolving();

		playerResultList.clear();
		computerResultList.clear();

		gameState.shuffle();

		gameState.startSolving();

		labelDices();
		updateDiceState(gameState.getLastMove());
		updateOkButton();
		gameState.startCountDown();
		updateScore();
	}

	private LetterButton[] letterButtons = new LetterButton[16];

	private HashMap<Integer, LetterButton> idToLetterButton = new HashMap<Integer, LetterButton>();

	final static int letterButtonIds[] = { R.id.button01, R.id.button02,
			R.id.button03, R.id.button04, R.id.button11, R.id.button12,
			R.id.button13, R.id.button14, R.id.button21, R.id.button22,
			R.id.button23, R.id.button24, R.id.button31, R.id.button32,
			R.id.button33, R.id.button34 };

	public void letterClick(View view) {
		LetterButton pressedButton = idToLetterButton.get(view.getId());
		int move = pressedButton.getPos();
		gameState.play(move);

		updateOkButton();
		updateDiceState(move);
	}

	public void okClick(View view) {
		String guess = gameState.getCurrentGuess();
		if (gameState.validatePlayerGuess(guess)) {
			playerResultList.insert(new Result(guess), 0);
		} else {
			guess = guess.replaceAll("Q", "QU");
			if (gameState.validatePlayerGuess(guess)) {
				playerResultList.insert(new Result(guess), 0);
			} else {
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
		okButton.setEnabled(gameState.getCurrentGuess().length() >= minLength);
	}

	private void updateScore() {
		if (scoreTextView != null)
			scoreTextView.setText("" + gameState.getPlayerScore() + " / "
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_item_info:
			showInfo();
			return true;
		case R.id.menu_item_prefs:
			showPreferences();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	static final int DIALOG_INFO = 0;

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_INFO:
			dialog = createInfoDialog();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private Dialog createInfoDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Info");
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		TextView textView = new TextView(this);
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		Spanned markup = Html
				.fromHtml("<H1>Word Finder v1.0</H1>&copy;"
						+ " <a href=\"mailto:Carsten.Friedrich@gmail.com?Subject=About%20Word%20Finder\">Carsten Friedrich</a>"
						+ " <br/><br/>License: <a href=\"http://www.gnu.org/licenses/gpl.html\">GPLv3</a><BR/>"
						+ "Acknowledgements:<BR/>Alan Beale for the <a href=\"http://wordlist.sourceforge.net/12dicts-readme.html\">12dicts</a> dictionaries used in this app.");

		fixMailtoLinks(markup);

		textView.setText(markup);
		builder.setView(textView);

		return builder.create();
	}

	private void fixMailtoLinks(Spanned markup) {
		try {
			SpannableStringBuilder markupString = (SpannableStringBuilder) markup;
			int start = -1;
			int max = markupString.length();
			while (true) {
				int nextPos = markupString.nextSpanTransition(start, max,
						URLSpan.class);
				URLSpan[] span = markupString.getSpans(nextPos, nextPos,
						URLSpan.class);
				if (span != null && span.length > 0) {
					for (URLSpan urlSpan : span) {
						if (urlSpan.getURL().toUpperCase()
								.startsWith("MAILTO:")) {
							MailToSpan rep = new MailToSpan(urlSpan);
							int s = markupString.getSpanStart(urlSpan);
							int e = markupString.getSpanEnd(urlSpan);
							int f = markupString.getSpanFlags(urlSpan);
							markupString.removeSpan(urlSpan);
							markupString.setSpan(rep, s, e, f);
						}
					}
				}
				start = nextPos;
				if (start == max)
					break;
			}
		} catch (ClassCastException e) {
			// ignore
		}
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