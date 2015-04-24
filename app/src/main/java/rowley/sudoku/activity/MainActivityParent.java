package rowley.sudoku.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.level_button:
                //todo
                break;
            case R.id.new_game_button:
                launchNewGame();
                break;
            case R.id.undo:
                //todo
                break;
        }
    }

    private void setDifficultyLevel(String levelString) {
        difficultyLevel = DifficultyLevel.getLevelForString(levelString);
        levelSelect.setText(levelString);

        SharedPrefsHelper.setDifficultyLevel(this, difficultyLevel);
    }

    private void launchNewGame() {
        gameDurationSeconds = 0;
        setTimeToClock();
        board.initializeBoard(difficultyLevel.getLevel());

        if(timerTask != null) {
            timerTask.cancel();
        }
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
        if(timerTask != null) {
            timerTask.cancel();
        }
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
        board.markCellAsPivot(isMarked, cellIndex);
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
