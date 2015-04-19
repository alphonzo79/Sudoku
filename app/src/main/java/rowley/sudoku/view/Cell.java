package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by joe on 4/19/15.
 */
public class Cell extends FrameLayout {

    private int squareWidth = 0;
    
    public Cell(Context context) {
        super(context);
    }

    public Cell(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Cell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
        final int width = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        if(width>this.squareWidth)
        {
            this.squareWidth = width;
        }
        setMeasuredDimension(this.squareWidth, this.squareWidth);
    }
}
