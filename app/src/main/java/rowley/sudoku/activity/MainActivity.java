package rowley.sudoku.activity;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import rowley.sudoku.R;
import rowley.sudoku.fragment.SetCellDialogFragment;
import rowley.sudoku.util.SharedPrefsHelper;
import rowley.sudoku.view.Board;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, Board.BoardListener,
        SetCellDialogFragment.SetCellFragListener {
    private Board board;
    private boolean warnOnBadEntry = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        warnOnBadEntry = SharedPrefsHelper.getProtectAgainstBadMoves(this);

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
        board.initializeBoard(1000000000);
    }

    @Override
    public void onShowSetCellFrag(boolean[] possibilities, int cellIndex) {
        SetCellDialogFragment frag = SetCellDialogFragment.newInstance(possibilities, cellIndex);
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
}
