package rowley.sudoku.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import rowley.sudoku.R;
import rowley.sudoku.fragment.MarkCellDialogFragment;
import rowley.sudoku.fragment.SetCellDialogFragment;
import rowley.sudoku.util.DifficultyLevel;
import rowley.sudoku.util.SharedPrefsHelper;
import rowley.sudoku.view.AlertMessageContents;
import rowley.sudoku.view.Board;
import rx.Observable;
import rx.Subscriber;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class MainActivityParent extends ActionBarActivity implements View.OnClickListener, Board.BoardListener,
        SetCellDialogFragment.SetCellFragListener, MarkCellDialogFragment.MarkCellFragListener {
    private Board board;
    private boolean warnOnBadEntry = false;
    private DifficultyLevel difficultyLevel;

    private boolean hasDismissedRegular = false;
    private boolean hasDismissedLong = false;

    private TextView clock, levelSelect, newGame;
    private View undo;

    private Timer timer;
    private TimerTask timerTask;
    private volatile int gameDurationSeconds = 0;
    private ClockUpdateHandler clockUpdateHandler;

    private boolean gameIsRunning = false;

    private Observable<Void> initializeBoardObservable;
    private Action1<Void> initialzeBoardOnNext;
    private Observable<Pair<Integer, Integer>> getHintObservable;
    private Action1<Pair<Integer, Integer>> getHintOnNext;
    private Observable<Boolean> flashHintLocationObservable;
    private Action1<Throwable> onError;
    private AlertDialog progressDialog;

    private final String BUNDLE_KEY_DURATION = "BUNDLE_KEY_DURATION";
    private final String BUNDLE_KEY_GAME_RUNNING = "BUNDLE_KEY_GAME_RUNNING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        warnOnBadEntry = SharedPrefsHelper.getProtectAgainstBadMoves(this);
        hasDismissedRegular = SharedPrefsHelper.getHasDismissedRegularClickEducation(this);
        hasDismissedLong = SharedPrefsHelper.getHasDismissedLongClickEducation(this);

        board = (Board)findViewById(R.id.board);
        board.setWarnOnBadEntry(warnOnBadEntry);

        clock = (TextView)findViewById(R.id.clock);
        levelSelect = (TextView)findViewById(R.id.level_button);
        levelSelect.setOnClickListener(this);
        newGame = (TextView)findViewById(R.id.new_game_button);
        newGame.setOnClickListener(this);
        undo = findViewById(R.id.undo);
        undo.setOnClickListener(this);

        clockUpdateHandler = new ClockUpdateHandler(this);
        timer = new Timer();

        String levelString = SharedPrefsHelper.getDifficultyLevelString(this);
        setDifficultyLevel(levelString);

        progressDialog = new ProgressDialog.Builder(this).setCancelable(false).create();

        initializeBoardObservable = Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                board.initializeBoard(difficultyLevel.getLevel());
                //Just in case it was too fast and the progress dialog only flashes
                //Make it look like it's working hard
                SystemClock.sleep(500);
                subscriber.onNext(null);
            }
        });

        initialzeBoardOnNext = new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                board.finalizeBoard();
                if(progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        };

        getHintObservable = Observable.create(new Observable.OnSubscribe<Pair<Integer, Integer>>() {
            @Override
            public void call(Subscriber<? super Pair<Integer, Integer>> subscriber) {
                Pair<Integer, Integer> hint = board.getHint();
                //Just in case it was too fast and the progress dialog only flashes
                //Make it look like it's working hard
                SystemClock.sleep(500);
                subscriber.onNext(hint);
            }
        });

        getHintOnNext = new Action1<Pair<Integer, Integer>>() {
            @Override
            public void call(Pair<Integer, Integer> hint) {
                if(progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                if(hint != null && hint.first >= 0 && hint.second > 0) {
                    setHintToCell(hint.first, hint.second);
                } else {
                    new AlertDialog.Builder(MainActivityParent.this).setCancelable(true).setMessage(R.string.no_hint_found)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
            }
        };

        onError = new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if(throwable != null) {
                    throwable.printStackTrace();
                }
                Toast.makeText(MainActivityParent.this, R.string.general_error, Toast.LENGTH_SHORT).show();
                if(progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        };

        if(savedInstanceState != null) {
            if(savedInstanceState.containsKey(BUNDLE_KEY_GAME_RUNNING)) {
                gameIsRunning = savedInstanceState.getBoolean(BUNDLE_KEY_GAME_RUNNING);
                gameDurationSeconds = savedInstanceState.getInt(BUNDLE_KEY_DURATION);
                board.restoreState(savedInstanceState);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if(gameIsRunning) {
            board.saveState(savedInstanceState);
            savedInstanceState.putInt(BUNDLE_KEY_DURATION, gameDurationSeconds);
            savedInstanceState.putBoolean(BUNDLE_KEY_GAME_RUNNING, gameIsRunning);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(gameIsRunning) {
            stopTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(gameIsRunning) {
            startTimer();
        }
    }

    @Override
    protected void onDestroy() {
        if(initializeBoardObservable != null) {
            initializeBoardObservable.unsubscribeOn(Schedulers.immediate());
        }
        if(getHintObservable != null) {
            getHintObservable.unsubscribeOn(Schedulers.immediate());
        }
        if(flashHintLocationObservable != null) {
            flashHintLocationObservable.unsubscribeOn(Schedulers.immediate());
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if(warnOnBadEntry) {
            menu.findItem(R.id.action_protect).setVisible(false);
            menu.findItem(R.id.action_dont_protect).setVisible(true);
        } else {
            menu.findItem(R.id.action_protect).setVisible(true);
            menu.findItem(R.id.action_dont_protect).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_protect:
                warnOnBadEntry = true;
                SharedPrefsHelper.setProtectAgainstBadMoves(this, warnOnBadEntry);
                board.setWarnOnBadEntry(warnOnBadEntry);
                return true;
            case R.id.action_dont_protect:
                warnOnBadEntry = false;
                SharedPrefsHelper.setProtectAgainstBadMoves(this, warnOnBadEntry);
                board.setWarnOnBadEntry(warnOnBadEntry);
                return true;
            case R.id.get_hint:
                getHint();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.level_button:
                showLevelSelector();
                break;
            case R.id.new_game_button:
                launchNewGame();
                break;
            case R.id.undo:
                if(gameIsRunning) {
                    if (!board.undo()) {
                        Toast.makeText(this, R.string.nothing_to_undo, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, R.string.start_game_before_undo, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void showLevelSelector() {
        final String[] levels = DifficultyLevel.getDisplayStrings();
        new AlertDialog.Builder(this).setCancelable(true).setTitle(R.string.choose_difficulty_level)
                .setSingleChoiceItems(levels, difficultyLevel.ordinal(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if(!levels[which].equals(difficultyLevel.getDisplayString())) {
                            setDifficultyLevel(levels[which]);
                            launchNewGame();
                        }
                    }
                }).show();
    }

    private void setDifficultyLevel(String levelString) {
        difficultyLevel = DifficultyLevel.getLevelForString(levelString);
        levelSelect.setText(levelString);

        SharedPrefsHelper.setDifficultyLevel(this, difficultyLevel);
    }

    private void launchNewGame() {
        gameDurationSeconds = 0;
        setTimeToClock();

        progressDialog.setMessage(getString(R.string.preparing_board));
        progressDialog.show();

        AppObservable.bindActivity(this, initializeBoardObservable).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(initialzeBoardOnNext, onError);

        stopTimer();
        startTimer();

        gameIsRunning = true;
    }

    private void stopTimer() {
        if(timerTask != null) {
            timerTask.cancel();
        }
    }

    private void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                clockUpdateHandler.sendEmptyMessage(0);
            }
        };
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);
    }

    private void setTimeToClock() {
        int minutes = gameDurationSeconds / 60;
        int seconds = gameDurationSeconds % 60;
        clock.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
    }

    private void getHint() {
        if(gameIsRunning) {
            progressDialog.setMessage(getString(R.string.looking_for_hint));
            progressDialog.show();

            AppObservable.bindActivity(this, getHintObservable).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(getHintOnNext, onError);
        } else {
            Toast.makeText(this, R.string.start_game_first, Toast.LENGTH_SHORT).show();
        }
    }

    private void setHintToCell(final int cellIndex, int foundOneBasedValue) {
        board.setNumToCell(foundOneBasedValue, cellIndex);
        //Let us return it to its original state
        final boolean isMarked = board.isCellMarkedAsPivot(cellIndex);
        board.markCellAsPivot(!isMarked, cellIndex, false);

        flashHintLocationObservable = Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                SystemClock.sleep(400);
                subscriber.onNext(isMarked);
                SystemClock.sleep(400);
                subscriber.onNext(!isMarked);
                SystemClock.sleep(400);
                subscriber.onNext(isMarked);
                SystemClock.sleep(400);
                subscriber.onNext(!isMarked);
                SystemClock.sleep(400);
                subscriber.onNext(isMarked);
            }
        });

        AppObservable.bindActivity(this, flashHintLocationObservable).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                board.markCellAsPivot(aBoolean, cellIndex, false);;
            }
        }, onError);
    }

    @Override
    public void onShowSetCellFrag(boolean[] possibilities, int cellIndex) {
        if (!hasDismissedRegular) {
            final boolean[] finalPossibilities = possibilities;
            final int finalCellIndex = cellIndex;

            final AlertMessageContents contents = new AlertMessageContents(this);
            contents.setMessage(getString(R.string.regular_education));

            new AlertDialog.Builder(this).setView(contents).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(contents.isDontShowChecked()) {
                        hasDismissedRegular = true;
                        SharedPrefsHelper.setHasDismissedRegularClickEducation(MainActivityParent.this);
                    }
                    dialog.cancel();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    showSetCellFrag(finalPossibilities, finalCellIndex);
                }
            }).show();
        } else {
            showSetCellFrag(possibilities, cellIndex);
        }
    }

    private void showSetCellFrag(boolean[] possibilities, int cellIndex) {
        SetCellDialogFragment frag = SetCellDialogFragment.newInstance(possibilities, cellIndex);
        frag.setCancelable(true);
        frag.show(getFragmentManager(), "");
    }

    @Override
    public void onShowMarkCellFrag(boolean[] markedPossibilities, boolean isMarked, int cellIndex) {
        if (!hasDismissedLong) {
            final boolean[] finalPossibilities = markedPossibilities;
            final boolean finalIsMarked = isMarked;
            final int finalCellIndex = cellIndex;

            final AlertMessageContents contents = new AlertMessageContents(this);
            contents.setMessage(getString(R.string.long_education));

            new AlertDialog.Builder(this).setView(contents).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(contents.isDontShowChecked()) {
                        hasDismissedLong = true;
                        SharedPrefsHelper.setHasDismissedLongClickEducation(MainActivityParent.this);
                    }
                    dialog.cancel();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    showMarkCellFrag(finalPossibilities, finalIsMarked, finalCellIndex);
                }
            }).show();
        } else {
            showMarkCellFrag(markedPossibilities, isMarked, cellIndex);
        }
    }

    private void showMarkCellFrag(boolean[] markedPossibilities, boolean isMarked, int cellIndex) {
        MarkCellDialogFragment frag = MarkCellDialogFragment.newInstance(markedPossibilities, isMarked, cellIndex);
        frag.setCancelable(true);
        frag.show(getFragmentManager(), "");
    }

    @Override
    public void onComplete() {
        stopTimer();

        gameIsRunning = false;

        Toast.makeText(this, "Good Job", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClear(int cellIndex) {
        board.clearCell(cellIndex);
    }

    @Override
    public void onCellSet(int oneBasedNumChosen, int cellIndex) {
        board.setNumToCell(oneBasedNumChosen, cellIndex);
    }

    @Override
    public void onSaveNotes(boolean[] markedPossibilities, int cellIndex) {
        board.setMarksToCell(markedPossibilities, cellIndex);
    }

    @Override
    public void onMarkCell(boolean isMarked, int cellIndex) {
        board.markCellAsPivot(isMarked, cellIndex, true);
    }

    static class ClockUpdateHandler extends Handler {
        private final WeakReference<MainActivityParent> activityRef;

        ClockUpdateHandler(MainActivityParent activity) {
            activityRef = new WeakReference<MainActivityParent>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            if(activityRef.get() != null) {
                activityRef.get().gameDurationSeconds++;
                activityRef.get().setTimeToClock();
            }
        }
    };
}
