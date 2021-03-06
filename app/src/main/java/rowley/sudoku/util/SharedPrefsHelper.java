package rowley.sudoku.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsHelper {
    private static final String SHARED_PREFS_NAME = "sudokuPrefs";
    private static final String PROTECT_AGAINST_BAD_MOVES = "protectAgainstBadMoves";
    private static final String DIFFICULTY_LEVEL = "difficulty";
    private static final String HAS_DISMISSED_REGULAR = "hasDismissedRegular";
    private static final String HAS_DISMISSED_LONG = "hasDismissedLong";

    public static boolean getProtectAgainstBadMoves(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PROTECT_AGAINST_BAD_MOVES, false);
    }

    public static void setProtectAgainstBadMoves(Context context, boolean protect) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PROTECT_AGAINST_BAD_MOVES, protect);
        editor.apply();
    }

    public static String getDifficultyLevelString(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(DIFFICULTY_LEVEL, DifficultyLevel.EASY.getDisplayString());
    }

    public static void setDifficultyLevel(Context context, DifficultyLevel level) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DIFFICULTY_LEVEL, level.getDisplayString());
        editor.apply();
    }

    public static boolean getHasDismissedRegularClickEducation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(HAS_DISMISSED_REGULAR, false);
    }

    public static void setHasDismissedRegularClickEducation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(HAS_DISMISSED_REGULAR, true);
        editor.apply();
    }

    public static boolean getHasDismissedLongClickEducation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(HAS_DISMISSED_LONG, false);
    }

    public static void setHasDismissedLongClickEducation(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(HAS_DISMISSED_LONG, true);
        editor.apply();
    }
}
