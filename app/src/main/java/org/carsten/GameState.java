/*
 * Copyright Carsten Friedrich (Carsten.Friedrich@gmail.com)
 *
 * License: GNU GENERAL PUBLIC LICENSE 3.0 (https://www.gnu.org/copyleft/gpl.html)
 *
 */
package org.carsten;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.os.CountDownTimer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

class GameState {
	final private Dictionary dictionary;

	Dictionary getDictionary() {
		return dictionary;
	}

	final private char[] board = new char[16];
	private WordFinder owner;

	void setOwner(WordFinder owner) {
		this.owner = owner;
	}

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
	private final static double[] letterFreqProbEnglish = { 0.08167, 0.09659, 0.12441, 0.16694,
			0.29396, 0.31624, 0.33639, 0.39733, 0.46699, 0.46852, 0.47624,
			0.51649, 0.54055, 0.60804, 0.68311, 0.7024, 0.70335, 0.76322,
			0.82649, 0.91705, 0.94463, 0.95441, 0.97801, 0.97951, 0.99925, 1 };

    private final static double[] letterFreqProbGerman = { 0.068050,
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
            1.000000 };

	GameState(WordFinder owner, Dictionary dictionary) {
		this.owner = owner;
		this.dictionary = dictionary;
	}

	final private ArrayList<Result> computerResultList = new ArrayList<>();
	final private ArrayList<Result> playerResultList = new ArrayList<>();

	@NonNull
    ArrayList<Result> getComputerResultList() {
		return computerResultList;
	}

	@NonNull
    ArrayList<Result> getPlayerResultList() {
		return playerResultList;
	}

	char getBoard(int move) {
		return board[move];
	}

	final private boolean[] playerTaken = new boolean[16];

    public boolean isTimeUp() {
        return timeUp;
    }

    public void setTimeUp(boolean timeUp) {
        this.timeUp = timeUp;
    }

    private boolean timeUp = false;

	@Nullable
    private SolveTask solver;

	void shuffle() {
		clearGuess();
		setTimeUp(false);
		for (int i = 0; i < 16; i++) {
			board[i] = pickRandomLetter();
		}
	}

	private char pickRandomLetter() {
		double r = Math.random();
		int i=0;

        double[] letterFreqProb = letterFreqProbEnglish;

		if(dictionaryName.equalsIgnoreCase("german")) {
            letterFreqProb = letterFreqProbGerman;
        }

		while(letterFreqProb[i]<r) i++;
		
		return (char) ('A'+i);
	}

	boolean findWord(String word) {
		word = word.toUpperCase().replaceAll("QU", "Q");
		boolean[] taken = new boolean[16];

        char[] chars = word.toCharArray();
		int index = 0;

		for (int i = 0; i < 16; i++)
			if (findWord(chars, index, taken, i))
				return true;
		return false;
	}

	private boolean findWord(@NonNull char[] chars, int index, boolean[] taken, int move) {
		if (taken[move])
			return false;
		if (chars[index] != board[move])
			return false;

		if (index == chars.length - 1)
			return true;

		taken[move] = true;

		for (int i = 0; i < WordFinder.MOVES[move].length; i++) {
			if (findWord(chars, index + 1, taken, WordFinder.MOVES[move][i]))
				return true;
		}

		taken[move] = false;

		return false;
	}

	void addComputerResult(Result result) {
		computerResultList.add(result);
		Collections.sort(computerResultList, (object1, object2) -> {
            int s1 = object1.toString().length();
            int s2 = object2.toString().length();
            int res = -Double.compare(s1, s2);
            if (res == 0)
                res = object1.toString().compareTo(object2.toString());
            return res;
        });

		if (owner != null)
			owner.updateComputerResultView();

	}

	void stopSolving() {
		if (solver != null) {
			solver.cancel(true);
			solver = null;
		}
	}

	void startSolving() {
		solver = new SolveTask(this);
		solver.execute("N/A");
	}

	boolean isAvailable(int i) {
		return !playerTaken[i];
	}

	void clearGuess() {
		currentGuess = "";
		lastMove = -1;
		Arrays.fill(playerTaken, false);
	}

	@NonNull
    private String currentGuess = "";

	@NonNull
    String getCurrentGuess() {
		return currentGuess;
	}

	private int lastMove = -1;
	private boolean allow3LetterWords = true;
	@NonNull
    private SCORE_ALG scoreAlg = SCORE_ALG.COUNT;
	private String dictionaryName;

	String getDictionaryName() {
		return dictionaryName;
	}

	boolean isAllow3LetterWords() {
		return allow3LetterWords;
	}

	int getLastMove() {
		return lastMove;
	}

	void play(int move) {
		lastMove = move;
		currentGuess = currentGuess + board[move];
		playerTaken[move] = true;
	}

	String validatePlayerGuess(@NotNull String guess) {
		int minLength = isAllow3LetterWords() ? 3 : 4;

		if(guess.length() < minLength) return owner.getString(R.string.WordGuessTooShort);

		for (Result result : playerResultList) {
			if (result.toString().equalsIgnoreCase(guess))
				return owner.getString(R.string.WordAlreadyFound);
		}
		if(getDictionary().lookup(guess, dictionaryName) == null) {
			return owner.getString(R.string.WordNotInDictionary);
		}

		return null;
	}

	void setDictionaryName(String string) {
		this.dictionaryName = string;
	}

	void setScoringAlgorithm(String string) {
		if ("count".equalsIgnoreCase(string)) {
			this.scoreAlg = SCORE_ALG.COUNT;
		} else {
			this.scoreAlg = SCORE_ALG.VALUE;
		}
	}

	void setAllow3LetterWords(boolean flag) {
		this.allow3LetterWords = flag;
		if (!isAllow3LetterWords()) {
			for (Iterator<Result> iter = playerResultList.iterator(); iter
					.hasNext();) {
				Result next = iter.next();
				if (next.toString().length() < 4)
					iter.remove();
			}

			for (Iterator<Result> iter = computerResultList.iterator(); iter
					.hasNext();) {
				Result next = iter.next();
				if (next.toString().length() < 4)
					iter.remove();
			}
		}
	}

	@Nullable
    private CountDownTimer countDownTimer = null;

    public long getCountDownTime() {
        return countDownTime;
    }

    private long countDownTime = -1;

	void setCountDownTime(long time) {
		countDownTime = time;
	}

	void startCountDown() {
        if (countDownTimer != null)
            countDownTimer.cancel();

		if (countDownTime < 0)
			return;

		if (owner != null)
			owner.updateTimeView(countDownTime);

		countDownTimer = new CountDownTimer(countDownTime, 1000) {

			public void onTick(long millisUntilFinished) {
				if (owner != null)
					owner.updateTimeView(millisUntilFinished / 1000);
			}

			public void onFinish() {
			    setTimeUp(true);
				if (owner != null)
					owner.updateTimeView(-1);
			}
		}.start();

	}

	private int getScore(@NonNull List<Result> list) {

		int res = 0;
		switch (this.scoreAlg) {
		case COUNT:
			res = list.size();
			break;
		case VALUE:
			for (Result result : list) {
				switch (result.toString().length()) {
				case 3:
				case 4:
					res += 1;
					break;
				case 5:
					res += 2;
					break;
				case 6:
					res += 3;
					break;
				case 7:
					res += 5;
					break;
				default:
					res += 11;
				}
			}
			break;
		}
		return res;
	}

	int getPlayerScore() {
		return getScore(playerResultList);
	}

    public boolean hasGameStarted() {
	    return board[0] != 0;
    }

    enum SCORE_ALG {
		COUNT, VALUE
	}

	int getComputerScore() {
		return getScore(computerResultList);
	}
}
