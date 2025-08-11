package org.leycm.chessbot.gui;

import org.leycm.chessbot.ChessBotApplication;

import javax.swing.*;
import java.awt.*;

public class ChessJFrameGame {

    private static DefaultListModel<String> listModel = new DefaultListModel<>();
    private static JFrame jFrame = new JFrame();
    private static JPanel jSidePanel = new JPanel(new BorderLayout());
    private static int oldWodth = 0;
    private static int oldHeight = 0;

    private static JPanel boardPanel = new JPanel(new GridLayout(8, 8)) {
        @Override
        public Dimension getPreferredSize() {
            int size = computeSquareSize(this);
            return new Dimension(size, size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            revalidate();
        }
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessJFrameGame::createAndShowGUI);
        new Timer(50, e -> {
            int width = jFrame.getWidth() - jFrame.getHeight();
            jSidePanel.setPreferredSize(new Dimension(width, jFrame.getHeight()));
            jSidePanel.revalidate();
            if (jFrame.getHeight() > jFrame.getWidth()) {
                jFrame.setSize(new Dimension(jFrame.getWidth(), jFrame.getWidth()));
            }
        }).start();
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Chess board [UI]");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());
        frame.setResizable(true);
        jFrame = frame;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JPanel square = new JPanel();
                if ((row + col) % 2 == 0) {
                    square.setBackground(Color.WHITE);
                } else {
                    square.setBackground(Color.BLACK);
                }
                boardPanel.add(square);
            }
        }

        JList<String> list = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(list);
        JLabel title = new JLabel("Match History", SwingConstants.CENTER);

        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(title, BorderLayout.NORTH);
        sidePanel.add(listScrollPane, BorderLayout.CENTER);
        jSidePanel = sidePanel;

        frame.add(boardPanel, BorderLayout.CENTER);
        frame.add(sidePanel, BorderLayout.EAST);

        frame.setVisible(true);
    }

    private static int computeSquareSize(JPanel panel) {
        int width = panel.getParent() != null ? panel.getParent().getWidth() : panel.getWidth();
        int height = panel.getParent() != null ? panel.getParent().getHeight() : panel.getHeight();
        if (width == 0 || height == 0) {
            return 400;
        }
        return Math.min(width, height);
    }

    public void addElement(String text) {
        listModel.addElement(text);
    }

}
