import java.util.stream.IntStream;
import java.util.Arrays;
import javafx.util.Pair;

public class ExtendedState extends State {

    public ExtendedState() {
        super();
    }

    public ExtendedState(State s) {
        super(s);
    }

    public void setNextPiece(int nextPiece) {
        this.nextPiece = nextPiece;
    }

    public int getAggregateHeight() {
        int aggregateHeight = IntStream.of(getTop()).sum();
        return aggregateHeight;
    }

    public double getNumHoles() {
        int[][] field = getField();
        int[] top = getTop();

        int numHoles = 0;
        for (int j = 0;  j < State.COLS;  j++) {
            if (top[j] != 0) {
                for (int i = top[j] - 1;  i >= 0;  i--) {
                    if (field[i][j] == 0) {
                        numHoles++;
                    }
                }
            }
        }
        return (double) numHoles * 10;
    }

    public double getRowTransitions() {
        int[][] field = getField();
        int rowTransitions = 0;
        int lastCell = 1;
        for (int i = 0;  i < State.ROWS;  i++) {
            for (int j = 0;  j < State.COLS;  j++) {
                if ((field[i][j] == 0) != (lastCell == 0)) {
                    rowTransitions++;
                }
                lastCell = field[i][j];
            }
            if (lastCell == 0) rowTransitions++;
        }
        return (double) rowTransitions;
    }

    public double getColTransitions() {
        int[][] field = getField();
        int[] top = getTop();
        int colTransitions = 0;
        for (int j = 0;  j < State.COLS;  j++) {
            for (int i = top[j] - 2;  i >= 0;  i--) {
                if ((field[i][j] == 0) != (field[i + 1][j] == 0)) {
                    colTransitions++;
                }
            }
            if (field[0][j] == 0 && top[j] > 0) colTransitions++;
        }
        return (double) colTransitions;
    }

    public int getBumpiness() {
        int[] top = getTop();

        int bumpiness = 0;
        for (int i = 0; i < COLS - 1; i++) {
            bumpiness += Math.abs(top[i] - top[i + 1]);
        }
        return bumpiness;
    }

    public int getHighestColumn() {
        int[] top = getTop();
        int highestCol = 0;
        for (int i = 0;  i < COLS;  i++) {
            highestCol = Math.max(highestCol, top[i]);
        }
        return highestCol;
    }

    public double getWellSum() {
        int[][] field = getField();
        int[] top = getTop();
        int wellSum = 0;
        for (int j = 0;  j < State.COLS;  j++) {
            for (int i = State.ROWS -1;  i >= 0;  i--) {
                if (field[i][j] == 0) {
                    if (j == 0 || field[i][j - 1] != 0) {
                        if (j == State.COLS - 1 || field[i][j + 1] != 0) {
                            int wellHeight = i - top[j] + 1;
                            wellSum += wellHeight * (wellHeight + 1) / 2;
                        }
                    }
                } else {
                    break;
                }
            }
        }
        return wellSum;
    }

    public double calculateValue(double[] gene) {
        double hole = getNumHoles();
        double rowTransition = getRowTransitions();
        double columnTransition = getColTransitions();
        double well = getWellSum();
        double cleared = getRowsCleared();

        double holeParam = gene[3];
        double rowTransitionParam = gene[1];
        double columnTransitionParam = gene[2];
        double wellParam = gene[4];
        double clearedParam = gene[0];

        cleared *= clearedParam;
        hole *= holeParam;
        rowTransition *= rowTransitionParam;
        columnTransition *= columnTransitionParam;
        well *= wellParam;

        return cleared + hole + rowTransition + columnTransition + well;
    }

    /** 
        Integer value is the number of dead moves (leading to end).
        Double value is the total values of all non-dead moves.
    */
    public Pair<Integer, Double> calculateValueWithLookahead(double[] gene) {
        int deadMoves = 0;
        double totalValue = 0;
        ExtendedState clonedState = new ExtendedState(this);

        for (int i = 0;  i < N_PIECES;  i++) {
            clonedState.setNextPiece(i);

            // Find best move if the next piece is i
            double tempBestValue = -Double.MAX_VALUE;
            int tempBestMove = -1;

            for (int move = 0;  move < clonedState.legalMoves().length;  move++) {
                ExtendedState lookAheadState = new ExtendedState(clonedState);
                lookAheadState.makeMove(move);

                if (lookAheadState.hasLost()) {
                    continue;
                }

                double value = lookAheadState.calculateValue(gene);
                if (value > tempBestValue) {
                    tempBestValue = value;
                    tempBestMove = move;
                }
            }

            if (tempBestMove == -1) {
                deadMoves++;
            } else {
                totalValue += tempBestValue;
            }
        }

        totalValue = calculateValue(gene);
        deadMoves = 0;
        return new Pair<>(deadMoves, totalValue);
    }

}