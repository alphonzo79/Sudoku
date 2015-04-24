package rowley.sudoku.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import rowley.sudoku.R;

/**
 * Created by joe on 4/23/15.
 */
public class AlertMessageContents extends LinearLayout {
    public AlertMessageContents(Context context) {
        super(context);
        initView(context);
    }

    public AlertMessageContents(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public AlertMessageContents(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.dialog_with_dont_show, this);
    }

    public void setMessage(String message) {
        ((TextView)findViewById(R.id.message)).setText(message);
    }

    public boolean isDontShowChecked() {
        return ((CheckBox)findViewById(R.id.checkbox)).isChecked();
    }
}
