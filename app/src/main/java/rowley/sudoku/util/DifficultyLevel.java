package rowley.sudoku.util;

public enum DifficultyLevel {
    EASY("Easy", 1000000D),
    MEDIUM("Medium", 10000000000D),
    DIFFICULT("Difficult", 1000000000000000000D),
    EXTREME("Extreme", 100000000000000000000000D);

    private DifficultyLevel(String displayString, double level) {
        this.display = displayString;
        this.level = level;
    }

    private String display;
    private double level;

    public String getDisplayString() {
        return display;
    }

    public double getLevel() {
        return level;
    }

    public static DifficultyLevel getLevelForString(String inString) {
        DifficultyLevel result = null;

        if(EASY.display.equals(inString)) {
            result = EASY;
        } else if(MEDIUM.display.equals(inString)) {
            result = MEDIUM;
        } else if(DIFFICULT.display.equals(inString)) {
            result = DIFFICULT;
        } else if(EXTREME.display.equals(inString)) {
            result = EXTREME;
        }

        return result;
    }
}
