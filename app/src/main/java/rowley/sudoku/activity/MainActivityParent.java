package rowley.sudoku.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        warnOnBadEntry = SharedPrefsHelper.getProtectAgainstBadMoves(this);
        String levelString = SharedPrefsHelper.getDifficultyLevelString(this);
        difficultyLevel = DifficultyLevel.getLevelForString(levelString);
        hasDismissedRegular = SharedPrefsHelper.getHasDismissedRegularClickEducation(this);
        hasDismissedLong = SharedPrefsHelper.getHasDismissedLongClickEducation(this);

        board = (Board)findViewById(R.id.board);
        board.setWarnOnBadEntry(warnOnBadEntry);

        findViewById(R.id.temp_button).setOnClickListener(this);
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
        board.initializeBoard(difficultyLevel.getLevel());
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
}
