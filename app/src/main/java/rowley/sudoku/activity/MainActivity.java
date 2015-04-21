package rowley.sudoku.activity;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import rowley.sudoku.R;
import rowley.sudoku.fragment.SetCellDialogFragment;
import rowley.sudoku.view.Board;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, Board.BoardListener,
        SetCellDialogFragment.SetCellFragListener {
    private Board board;
    private boolean warnOnBadEntry = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
    public void onClear(int cellIndex) {
        board.clearCell(cellIndex);
    }

    @Override
    public void onCellSet(int oneBasedNumChosen, int cellIndex) {
        board.setNumToCell(oneBasedNumChosen, cellIndex);
    }
}
