/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carstenf.wordfinder;

import java.util.HashSet;

import android.database.Cursor;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.util.Log;

/**
 * @author carsten.friedrich@gmail.com
 * 
 */
class SolveTask extends AsyncTask<String, String, String> {
	private HashSet<String> prefixes;
    final private GameState gameState;

	SolveTask(GameState gameState) {
		this.gameState = gameState;
	}
	
	@Nullable
    @Override
	protected String doInBackground(String... params) {
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		solve2();
		return null;
	}

	private void solve2() {
		prefixes = new HashSet<>();
		boolean[] taken = new boolean[16];
		for (int i = 0; i < 16; i++) {
			findAnyWord(i, taken, 2, "");
		}

		for (String prefix : prefixes) {
			solve1(prefix);
		}
	}

	private void findAnyWord(int move, boolean[] taken, int depth,
			String res) {
		taken[move] = true;
		if (depth == 0) {
			Log.i(WordFinder.TAG, res + gameState.getBoard(move));
			prefixes.add(res + gameState.getBoard(move));
		} else {
			for (int next : WordFinder.MOVES[move]) {
				if (!taken[next])
					findAnyWord(next, taken, depth - 1, res + gameState.getBoard(move));
			}
		}
		taken[move] = false;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if (isCancelled())
			return;
		super.onProgressUpdate(values);
		gameState.addComputerResult(new Result(values[0]));
	}

	private void solve1(String prefix) {
		Cursor cursor = gameState.getDictionary().getAllWords(prefix, gameState.getDictionaryName());
        try (cursor) {
            if (cursor == null)
                return;
            cursor.moveToFirst();
            do {
                String word = cursor.getString(0);
                int minLength = gameState.isAllow3LetterWords() ? 3 : 4;
                if (word.length() >= minLength && gameState.findWord(word)) {
                    Log.d(WordFinder.TAG, "Found: " + word);
                    publishProgress(word);
                }
            } while (cursor.moveToNext());
        }
	}
}