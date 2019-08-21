package com.abhyudayasharma.tictactoe;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

@ParametersAreNonnullByDefault
class Game {
    private PlayerType currentPlayer = null;
    private String userChar;
    private String compChar;
    private final static int size = 3;
    private final JFrame gameFrame = new JFrame("Tic Tac Toe");
    private final Vector<Integer> humanMoves = new Vector<>(9) {
        @Override
        public synchronized boolean add(Integer integer) {
            boolean ret = super.add(integer);
            humanMovesList.setListData(this);
            return ret;
        }
    };
    private final Vector<Integer> computerMoves = new Vector<>(9) {
        @Override
        public synchronized boolean add(Integer integer) {
            boolean ret = super.add(integer);
            computerMovesList.setListData(this);
            return ret;
        }
    };
    private final JList<Integer> humanMovesList = new JList<>();
    private final JList<Integer> computerMovesList = new JList<>();
    private JButton[][] buttons = new JButton[size][size];
    private boolean flag = true;
    private static int humanWins = 0;
    private static int computerWins = 0;
    private JPanel boxes = new JPanel();
    private static String leader = "Nobody";

    @Nonnegative
    private final int[][] magicSquare = MagicSquare.getMagicSquare(size);
    private final int winningSum = MagicSquare.getExpectedSum();

    static {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            Logger.getLogger(Game.class.getName()).log(Level.WARNING, "Unable to set Nimbus Look and Feel", e);
        }
    }

    void start() {
        gameFrame.setBackground(Color.BLACK);


        boxes.setLayout(new GridLayout(3, 3, 5, 5));
        boxes.setBackground(Color.BLACK);
        boxes.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; ++j) {
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 44));
                buttons[i][j].setBackground(Color.WHITE);
                buttons[i][j].setPreferredSize(new Dimension(100, 100));
                addOnClickListener(buttons[i][j], i, j);
                boxes.add(buttons[i][j]);
                boxes.setBackground(Color.WHITE);
            }
        }

        JPanel lists = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 0;

        JLabel scoreLabel = new JLabel();
        scoreLabel.setText("SCORE: " + humanWins + " - " + computerWins);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        lists.add(scoreLabel, gbc);

        gbc.gridy++;
        JLabel leaderLabel = new JLabel();
        leaderLabel.setText("Leader: " + leader);
        leaderLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        lists.add(leaderLabel, gbc);

        gbc.gridy++;
        lists.add(new JLabel("Human moves"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        var humanMovesScrollPane = new JScrollPane(humanMovesList);
        humanMovesScrollPane.setPreferredSize(new Dimension(100, 100));
        lists.add(humanMovesScrollPane, gbc);

        gbc.gridy++;
        lists.add(new JLabel("Computer moves"), gbc);

        gbc.gridy++;
        var computerMovesScrollPane = new JScrollPane(computerMovesList);
        computerMovesScrollPane.setPreferredSize(new Dimension(100, 100));
        lists.add(computerMovesScrollPane, gbc);
        lists.setBackground(Color.WHITE);

        computerMovesList.setEnabled(false);
        humanMovesList.setEnabled(false);

        gameFrame.setLayout(new BoxLayout(gameFrame.getContentPane(), BoxLayout.LINE_AXIS));
        gameFrame.setResizable(false);
        gameFrame.add(boxes);
        gameFrame.add(lists);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setVisible(true);
        gameFrame.pack();
        gameFrame.setLocationRelativeTo(null);

        // Who starts first?
        PlayerType startPlayer = findStartPlayer();
        if (startPlayer == null) {
            gameFrame.dispose();
        }
        currentPlayer = startPlayer;
        if (startPlayer == PlayerType.HUMAN) {
            userChar = "X";
            compChar = "O";
        } else {
            userChar = "O";
            compChar = "X";
            userMove();
        }
        gameFrame.requestFocus();
    }

    private void addOnClickListener(JButton button, int row, int col) {
        button.addActionListener(e -> {
            flag = true;
            button.setEnabled(false);
            gameFrame.requestFocus();

            button.setText(userChar);
            humanMoves.add(magicSquare[row][col]);
            // disable the frame to avoid clicks when we simulate calculation
            gameFrame.setEnabled(false);

            // delay the response of the computer
            Timer timer = new Timer(500, l -> {
                // Check if human has won
                boolean win = humanWin(humanMoves);
                if (win) {
                    gameOver("YOU");
                    flag = false;
                } else {
                    if (humanMoves.size() + computerMoves.size() == 9) {
                        gameOver("NOBODY");
                    }

                    userMove();
                }
                // re-enable the frame after the computer is done
                gameFrame.setEnabled(true);
            });

            timer.setRepeats(false);
            timer.start();
        });
    }

    private void userMove() {
        // Win: If you have two in a row, play the third to get three in a row.
        int winEntry;
        if (flag) {
            winEntry = possWin(humanMoves, computerMoves);
            if (winEntry > 0 && winEntry < 10) {
                computerMoves.add(winEntry);
                findAndDisable(winEntry);
                gameOver("COMPUTER");
                flag = false;
            }
        }

        //Block: If the opponent has two in a row, play the third to block them.
        if (flag) {
            winEntry = possWin(computerMoves, humanMoves);
            if (winEntry > 0 && winEntry < 10) {
                computerMoves.add(winEntry);
                findAndDisable(winEntry);
                flag = false;
            }
        }

        //Center: Play the center
        int centre = magicSquare[size / 2][size / 2];
        if (flag) {
            if (!(humanMoves.contains(centre) || computerMoves.contains(centre))) {
                computerMoves.add(centre);
                findAndDisable(centre);
                flag = false;
            }
        }

        //Block Opponent's Forks

        //Block centre and corner move
        if (flag) {
            int[] temp = {2, 4, 6, 8};
            if (humanMoves.size() == 2 && humanMoves.contains(centre) && elementCounter(humanMoves, temp) == 1) {
                if (emptyCorner()) {
                    flag = false;
                }
            }
        }

        //Block two corner move
        if (flag) {
            int[] temp = {2, 4, 6, 8};
            if (humanMoves.size() == 2 && elementCounter(humanMoves, temp) == 2) {
                if (emptySide()) {
                    flag = false;
                }
            }
        }

        //Block two side move
        if (flag) {
            int[] temp = {1, 3, 7, 9};
            if (humanMoves.size() == 2 && elementCounter(humanMoves, temp) == 2) {
                int move = correctCorner(humanMoves);
                if (move != 0) {
                    computerMoves.add(move);
                    findAndDisable(move);
                    flag = false;
                }
            }
        }

        //Opposite Corner: If the opponent is in the corner, play the opposite corner.
        if (flag) {
            if (oppositeCorner()) {
                flag = false;
            }
        }

        //Empty Corner: Play an empty corner.
        if (flag) {
            if (emptyCorner()) {
                flag = false;
            }
        }

        //Empty Side: Play an empty side.
        if (flag) {
            if (emptySide()) {
                flag = false;
            }
        }

        currentPlayer = getNextPlayer();
    }

    private void gameOver(String winner) {
        boxes.setEnabled(false);
        if (winner.equalsIgnoreCase("Computer")) {
            computerWins++;
        } else if (winner.equalsIgnoreCase("You")) {
            humanWins++;
        }

        if (humanWins > computerWins)
            leader = "You";
        else if (computerWins > humanWins)
            leader = "Computer";
        else
            leader = "Nobody";
        int result = JOptionPane.showConfirmDialog(gameFrame, "Game Over! " + winner + " WON.\nThe score is now " + humanWins + " - " + computerWins + "."
                + "\nCurrent Leader: " + leader + "\nWould you like to play again?", "Game Over",
            JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            gameFrame.dispose();
            new Game().start();
        } else {
            gameFrame.setVisible(true);
        }
//        gameFrame.dispose();
    }

    @CheckForNull
    private PlayerType findStartPlayer() {
        int response = JOptionPane.showOptionDialog(gameFrame, "Welcome to Tic Tac Toe!\n" +
                "Would you like to start first?",
            "Tic Tac Toe", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[]{"Yes", "No"}, "Yes");
        if (response == JOptionPane.CLOSED_OPTION) {
            return null;
        }

        if (response == JOptionPane.YES_OPTION) {
            return PlayerType.HUMAN;
        } else {
            return PlayerType.COMPUTER;
        }
    }

    private int possWin(List Opponent, List Player) {
        if (Player.size() <= 1)
            return 0;
        int lastMove = (int) Player.get(Player.size() - 1);
        for (int i = 0; i < Player.size() - 1; ++i) {
            int diff = winningSum - ((int) Player.get(i) + lastMove);
            if (!(Opponent.contains(diff) || diff < 1 || diff > 9 || Player.contains(diff))) {
                return diff;
            }
        }
        return 0;
    }

    private void findAndDisable(int num) {
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                if (magicSquare[i][j] == num) {
                    buttons[i][j].setText(compChar);
                    buttons[i][j].setEnabled(false);
                }
            }
        }
    }

    @Nonnull
    private PlayerType getNextPlayer() {
        switch (currentPlayer) {
            case HUMAN:
                return PlayerType.COMPUTER;
            case COMPUTER:
                return PlayerType.HUMAN;
            default:
                throw new IllegalArgumentException("Unknown PlayerType");
        }
    }

    private boolean humanWin(List Player) {
        if (Player.size() <= 2)
            return false;
        int lastMove = (int) Player.get(Player.size() - 1);
        for (int i = 0; i < Player.size() - 2; ++i) {
            for (int j = i + 1; j < Player.size() - 1; ++j) {
                int sum = (int) Player.get(i) + (int) Player.get(j) + lastMove;
                if (sum == winningSum)
                    return true;
            }
        }
        return false;
    }

    private boolean oppositeCorner() {
        for (int i = humanMoves.size() - 1; i >= 0; --i) {
            if (fillOppositeCorner(humanMoves.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean emptyCorner() {
        int[] temp1 = new int[]{0, 0, size - 1, size - 1};
        int[] temp2 = new int[]{0, size - 1, 0, size - 1};
        return fillFirstFound(temp1, temp2);
    }

    private boolean emptySide() {
        int[] temp1 = new int[]{0, 1, 1, 2};
        int[] temp2 = new int[]{1, 0, 2, 1};
        return fillFirstFound(temp1, temp2);
    }

    private boolean fillFirstFound(int[] temp1, int[] temp2) {
        for (int i = 0, j = 0; i < temp1.length && j < temp2.length; ++i, ++j) {
            int num = magicSquare[temp1[i]][temp2[j]];
            if (!(humanMoves.contains(num) || computerMoves.contains(num))) {
                buttons[temp1[i]][temp2[j]].setText(compChar);
                buttons[temp1[i]][temp2[j]].setEnabled(false);
                computerMoves.add(num);
                return true;
            }
        }
        return false;
    }

    private boolean fillOppositeCorner(int num) {
        for (int i = 0; i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                if (magicSquare[i][j] == num) {
                    if (i == 0 && j == 0) {
                        if (buttons[size - 1][size - 1].isEnabled()) {
                            buttons[size - 1][size - 1].setText(compChar);
                            buttons[size - 1][size - 1].setEnabled(false);
                            computerMoves.add(magicSquare[size - 1][size - 1]);
                            return true;
                        }
                    } else if (i == size - 1 && j == 0) {
                        if (buttons[0][size - 1].isEnabled()) {
                            buttons[0][size - 1].setText(compChar);
                            buttons[0][size - 1].setEnabled(false);
                            computerMoves.add(magicSquare[0][size - 1]);
                            return true;
                        }
                    } else if (i == 0 && j == size - 1) {
                        if (buttons[size - 1][0].isEnabled()) {
                            buttons[size - 1][0].setText(compChar);
                            buttons[size - 1][0].setEnabled(false);
                            computerMoves.add(magicSquare[size - 1][0]);
                            return true;
                        }
                    } else if (i == size - 1 && j == size - 1) {
                        if (buttons[0][0].isEnabled()) {
                            buttons[0][0].setText(compChar);
                            buttons[0][0].setEnabled(false);
                            computerMoves.add(magicSquare[0][0]);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private int elementCounter(List Player, int[] arr) {
        int cnt = 0;
        for (int i1 : arr) {
            if (Player.contains(i1))
                cnt++;
        }
        return cnt;
    }

    private int correctCorner(List Player) {
        if (Player.contains(1)) {
            if (Player.contains(3))
                return 8;
            if (Player.contains(7))
                return 6;
        }
        if (Player.contains(9)) {
            if (Player.contains(3))
                return 4;
            if (Player.contains(7))
                return 2;
        }
        return 0;
    }
}
