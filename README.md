# Java Sudoku Solver

制約プログラミングのテクニックを用いた数独ソルバです。下記コマンドを
実行します。

    $ java SudokuSolver <input file name>

実行後、解答が存在する場合はinputファイル名の拡張子が .out に置き換わった
ファイルに解答が出力されます。また解が存在しない場合は、コンソールに解が
存在しない旨のメッセージが出力されます。

入力ファイルは、各数字の間に一つ以上の空白文字を入れて下さい。
また入力データそのものに、不整合(e.g., 同一列内に同じ数字が付与されている等)
があった場合はコンソールに “入力データに問題があります”と表示されます。

    例： 1 2 3 4 5 6 7 8 9 => OK
         123456789         => INVALID

サンプルとして sample.in で定義される数独問題例を test ディレクトリに添付
しました。

【動作確認実行環境】

    $ java -version

    java version "1.6.0_65"
    Java(TM) SE Runtime Environment (build 1.6.0_65-b14-462-11M4609)
    Java HotSpot(TM) 64-Bit Server VM (build 20.65-b04-462, mixed mode)