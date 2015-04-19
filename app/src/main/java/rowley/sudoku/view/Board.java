package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import rowley.sudoku.R;

/**
 * Created by joe on 4/19/15.
 */
public class Board extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {
    private Cell[] cells = new Cell[81];
    private int[] winningBoard = new int[81];
    private int moveIndex = 0;
    private int[] moveRecord = new int[81];

    public Board(Context context) {
        super(context);
    }

    public Board(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Board(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_board, this);

        for(int i = 0; i < cells.length; i++) {
            Cell cell = (Cell)findViewWithTag(i);
            cell.setOnClickListener(this);
            cell.setOnLongClickListener(this);
            cells[i] = cell;
        }
    }

    public void initializeBoard() {
        moveIndex = 0;
        for(int i = 0; i < moveRecord.length; i++) {
            moveRecord[i] = -1;
        }
        //todo use the cells to figure the board, then transfer it to the winning board variable.
    }

    @Override
    public void onClick(View v) {
        //todo
    }

    @Override
    public boolean onLongClick(View v) {
        //todo
        return false;
    }
}
