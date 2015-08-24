/**
 * Sudoku Solver - Elimination によって数独を解く
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class SudokuSolver {
    /**
     * 入力ファイルに不整合があった場合の例外クラス
     */
    private static final class FileFormatException extends Exception {}

    /**
     * 数独セルクラス
     */
    private static final class Cell {
        // 数独サイズ
        private int size;
        // セルに数字が付与されている場合 true
        private boolean filled;
        // セルに付与可能な数字候補 (index 番のビットが true の場合、整数値 index + 1 を付与可能)
        private BitSet candidates;

        public Cell(int size) {
            this.size = size;
            this.filled = false;
            this.candidates = new BitSet(this.size);
            for (int i = 0; i < this.size; i++) {
                this.candidates.set(i);
            }
        }

        public Cell(Cell that) {
            this.size = that.size;
            this.filled = that.filled;
            this.candidates = new BitSet(that.size);
            this.candidates.or(that.candidates);
        }

        // セルに数字を付与
        public Cell set(int value) {
            if (this.check(value)) {
                this.filled = true;
                this.mask(value);
            }
            return this;
        }

        // セルの数字を取得
        public int get() {
            return this.candidates.cardinality() != 1 ? 0 : this.candidates.nextSetBit(0) + 1;
        }

        // セルに付与可能な数字候補数
        public int cardinality() {
            return this.candidates.cardinality();
        }

        // セルに付与可能な数字候補を絞り込み
        public Cell eliminate(int ... candidates) {
            BitSet _candidates = new BitSet(this.size);
            for (int candidate : candidates) {
                if (this.check(candidate)) {
                    _candidates.set(candidate - 1);
                }
            }
            return this.eliminate(_candidates);
        }

        // セルに付与可能な数字候補を絞り込み
        public Cell eliminate(BitSet candidates) {
            this.candidates.andNot(candidates);
            return this;
        }

        // セルによって定義される制約条件の取得
        public BitSet constraint() {
            return this.candidates;
        }

        // セルに付与可能な数字一覧をイテレータとして取得
        public Iterator<Integer> candidates() {
            return new Iterator<Integer>() {
                private Integer current = -1;

                @Override
                public boolean hasNext() {
                    return (current < size && (current = candidates.nextSetBit(current + 1)) >= 0);
                }

                @Override
                public Integer next() {
                    return current + 1;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        // セルに数字を付与済みの場合 true
        public boolean filled() {
            return this.filled;
        }

        // 引数で指定された数字が付与可能な場合 true
        public boolean accept(int value) {
            if (this.check(value)) {
                return this.candidates.get(value - 1);
            }
            return false;
        }

        // セルに付与可能な数字候補が無い場合 true
        public boolean contradict() {
            return this.candidates.cardinality() == 0;
        }

        // value 番地のみセットされた BistSet で数字候補をマスク
        private Cell mask(int ... mask) {
            BitSet _mask = new BitSet(this.size);
            for (int bit : mask) {
                _mask.set(bit - 1);
            }
            return this.mask(_mask);
        }

        // mask で数字候補をマスク
        private Cell mask(BitSet mask) {
            this.candidates.and(mask);
            return this;
        }

        // 数独サイズに適合した数字の場合 true
        private boolean check(int value) {
            return 0 < value && value <= this.size;
        }
    }

    /**
     * 数独ボードクラス
     */
    private static final class Board {
        // デフォルト数独次数
        private static final int DEFAULT_ORDER = 3;
        // デフォルト入力データセパレータ
        private static final String DEFAULT_SEPARATOR = "\\s+";
        // 数独を成すセル
        private final Cell[][] cells;
        // 数独次数
        private final int order;
        // 数独サイズ
        private final int size;

        public Board(File file) throws FileFormatException, IOException, NumberFormatException {
            this(DEFAULT_ORDER, file);
        }

        public Board(int order, File file) throws FileFormatException, IOException, NumberFormatException {
            this.order = order;
            this.size = this.order * this.order;
            this.cells = new Cell[this.size][this.size];

            for (int row = 0; row < this.size; row++) {
                for (int column = 0; column < this.size; column++) {
                    this.cells[row][column] = new Cell(this.size);
                }
            }

            final BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                int row = 0;
                for (String line = input.readLine(); line != null || row < this.size; line = input.readLine(), row++) {
                    String[] tokens = split(line);
                    if (tokens.length != this.size || row >= this.size) {
                        throw new FileFormatException();
                    }
                    for (int column = 0; column < this.size; column++) {
                        int value = Integer.parseInt(tokens[column]);
                        if (value != 0 && !this.accept(row, column, value)) {
                            throw new FileFormatException();
                        } else if (value != 0) {
                            this.set(row, column, value);
                        }
                    }
                }
            } finally {
                input.close();
            }
        }

        public Board(Board that) {
            this.size = that.size;
            this.order = that.order;
            this.cells = new Cell[this.size][this.size];
            for (int row = 0; row < this.size; row++) {
                for (int column = 0; column < this.size; column++) {
                    this.cells[row][column] = new Cell(that.cells[row][column]);
                }
            }
        }

        // row 行 column 列番地のセルに数字 value をセット
        public Board set(int row, int column, int value) {
            this.cells[row][column].set(value);
            this.propagate(row, column, value);
            return this;
        }

        // row 行 column 列番地のセルに付与されている数字を取得 (未付与の場合は 0 を返す)
        public int get(int row, int column) {
            if (this.cells[row][column].filled()) {
                return this.cells[row][column].get();
            }
            return 0;
        }

        // row 行 column 列番地のセルへの参照
        public Cell cell(int row, int column) {
            return this.cells[row][column];
        }

        // row 行 column 列番地のセルに付与可能な数字候補を絞り込み
        public Board eliminate(int row, int column, int ... candidates) {
            BitSet _candidates = new BitSet(this.size);
            for (int candidate : candidates) {
                _candidates.set(candidate - 1);
            }
            return this.eliminate(row, column, _candidates);
        }

        // row 行 column 列番地のセルに付与可能な数字候補を絞り込み
        public Board eliminate(int row, int column, BitSet candidates) {
            this.cells[row][column].eliminate(candidates);
            return this;
        }

        // row 行 column 列番地のセルが未付与で付与可能数字が唯一の場合は値を固定
        public void fix(int row, int column) {
            Cell cell = this.cells[row][column];
            if (!cell.filled() && cell.cardinality() == 1) {
                this.set(row, column, cell.get());
            }
        }

        // row 行 column 列番地に数字 value を付与可能な場合 true
        public boolean accept(int row, int column, int value) {
            return this.cells[row][column].accept(value);
        }

        // 付与された数字間で制約条件に違反があった場合 true
        public boolean contradict() {
            for (int row = 0; row < this.size(); row++) {
                for (int column = 0; column < this.size(); column++) {
                    if (this.cells[row][column].contradict()) {
                        return true;
                    }
                }
            }
            return false;
        }

        // 数独サイズの取得
        public int size() {
            return this.size;
        }

        // row 行 column 列番地に数字 value を付与した場合の制約条件を付与
        private void propagate(int row, int column, int value) {
            BitSet exclusion = new BitSet(this.size);
            exclusion.set(value - 1);
            int blockX = row / this.order * this.order;
            int blockY = column / this.order * this.order;
            for (int i = 0, x = blockX, y = blockY; i < this.size; i++, x = blockX + i / this.order, y = blockY + i % this.order) {
                if (i != column) {
                    this.eliminate(row, i, exclusion);
                }
                if (i != row) {
                    this.eliminate(i, column, exclusion);
                }
                if (x != row && y != column) {
                    this.eliminate(x, y, exclusion);
                }
            }
        }

        // デフォルト入力データセパレータで row を分割
        private static String[] split(String row) {
            return row.trim().split(DEFAULT_SEPARATOR);
        }
    }

    // 引数で与えられる数独を解く
    private static Board solve(Board board) {
        while (reasoning(board)) {
            // DO NOTHING
        }
        return search(board);
    }

    // 各種数独のテクニックを用いて各セルの付与可能数字候補を枝狩り
    private static boolean reasoning(Board board) {
        boolean reasoning = false;
        for (int row = 0; row < board.size(); row++) {
            BitSet constraint = null;
            for (int column = 0, tmp = -1; column < board.size(); column++) {
                Cell cell = board.cell(row, column);
                // Naked Singleton
                if (cell.cardinality() == 1 && !cell.filled()) {
                    reasoning = true;
                    board.fix(row, column);
                // Naked Pair
                } else if (cell.cardinality() == 2) {
                    if (tmp >= 0 && constraint != null && cell.constraint().equals(constraint)) {
                        for (int applyingColumn = 0; applyingColumn < board.size(); applyingColumn++) {
                            if (applyingColumn != tmp && applyingColumn != column) {
                                board.eliminate(row, applyingColumn, constraint);
                            }
                        }
                        tmp = -1;
                        constraint = null;
                    }
                    tmp = column;
                    constraint = cell.constraint();
                }
            }
        }
        return reasoning;
    }

    // 枝狩りされた選択肢から数独を総当たりで解く
    private static Board search(Board board) {
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                Cell cell = board.cell(row, column);
                if (!cell.filled()) {
                    Board cache = null;
                    Iterator<Integer> candidates = cell.candidates();
                    while (candidates.hasNext()) {
                        cache = solve(new Board(board).set(row, column, candidates.next()));
                        if (cache != null) {
                            break;
                        }
                    }
                    return cache;
                }
            }
        }
        return board;
    }

    // ファイルに数独を保存
    private static void save(Board board, File file) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(file));
        try {
            for (int row = 0; row < board.size(); row++) {
                StringBuilder sb = new StringBuilder();
                for (int column = 0; column < board.size(); column++) {
                    sb.append(" " + board.get(row, column));
                }
                output.write((sb.toString()).trim());
                output.newLine();
            }
        } finally {
            output.flush();
            output.close();
        }
    }

    // デバッグ用数独データ出力
    private static void debug(Board board) {
        System.out.print("\n");
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                System.out.print(" " + board.get(row, column) + " ");
                if (column == board.size() - 1) {
                    System.out.print("\n");
                }
            }
        }
        System.out.print("\n");
    }

    /**
     * 数独問題を解く
     * @param args args[0] 数独入力データファイル名
     */
    public static final void main(String[] args) {
        if (args.length > 0) {
            try {
                final Board problem = new Board(new File(args[0]));
                debug(problem);
                final long start = System.nanoTime();
                final Board answer = solve(problem);
                final long end = System.nanoTime();
                System.out.println("実行時間: " + ((end - start) / 1e6) + " (msec)");
                if (answer != null && !answer.contradict()) {
                    System.out.println("解が求まりました");
                    debug(answer);
                    final String fileName = args[0].substring(0, args[0].lastIndexOf('.')) + ".out";
                    save(answer, new File(fileName));
                } else {
                    System.out.println("解が存在しません");
                }
            } catch (final FileFormatException ex) {
                System.out.println("入力データに誤りがあります");
            } catch (final IOException ex) {
                System.out.println("ファイル入出力に問題があります");
            } catch (final NumberFormatException ex) {
                System.out.println("入力データに誤りがあります");
            }
        } else {
            System.out.println("入力ファイルを指定して下さい");
        }
    }
}
