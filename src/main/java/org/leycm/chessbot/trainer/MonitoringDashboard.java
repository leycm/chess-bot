package org.leycm.chessbot.trainer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class MonitoringDashboard extends JFrame {
    private static final int MAX_DATA_POINTS = 100;
    private List<ChartPanel> chartPanels;
    private JPanel statusPanel;
    private JTextArea infoArea;

    private List<List<Integer>> chartData;
    private final String[] chartNames = {"CPU Usage", "Memory", "Disk I/O", "Network", "Temperature", "Power"};

    public MonitoringDashboard() {
        initializeComponents();
        setupLayout();
        startUpdateTimer();
    }

    private void initializeComponents() {
        setTitle("System Monitoring Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        chartData = new ArrayList<>();
        chartPanels = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            chartData.add(new ArrayList<>());
            ChartPanel panel = new ChartPanel(chartNames[i]);
            chartPanels.add(panel);
        }

        statusPanel = new JPanel(new GridLayout(6, 2, 10, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Current Values"));

        infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        infoArea.setBorder(BorderFactory.createTitledBorder("System Info"));
        infoArea.setText("""
                System Monitoring Dashboard
                Runtime: 0 seconds
                Updates: 0
                Status: Running
                
                Monitoring 6 parameters:
                - CPU Usage (%)
                - Memory Usage (MB)
                - Disk I/O (KB/s)
                - Network (KB/s)
                - Temperature (°C)
                - Power Consumption (W)""");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel chartsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        chartsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (ChartPanel panel : chartPanels) {
            chartsPanel.add(panel);
        }

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusPanel, BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(infoArea), BorderLayout.EAST);
        bottomPanel.setPreferredSize(new Dimension(getWidth(), 150));

        add(chartsPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void startUpdateTimer() {
        Timer updateTimer = new Timer(1000, new ActionListener() {
            private int updateCount = 0;
            private final long startTime = System.currentTimeMillis();

            @Override
            public void actionPerformed(ActionEvent e) {
                updateCount++;

                updateSystemData(updateCount);

                long runtime = (System.currentTimeMillis() - startTime) / 1000;
                updateInfoArea(runtime, updateCount);

                repaint();
            }
        });
        updateTimer.start();
    }

    public void updateSystemData(int @NotNull [] values) {
        if (values.length != 6) {
            throw new IllegalArgumentException("Expected 6 values for the charts");
        }

        for (int i = 0; i < 6; i++) {
            List<Integer> data = chartData.get(i);
            data.add(values[i]);

            if (data.size() > MAX_DATA_POINTS) {
                data.remove(0);
            }

            chartPanels.get(i).updateData(data);
        }

        updateStatusPanel(values);
    }

    private void updateSystemData(int updateCount) {
        int[] simulatedValues = new int[6];
        simulatedValues[0] = (int)(Math.random() * 100); // CPU %
        simulatedValues[1] = (int)(2000 + Math.random() * 2000); // Memory MB
        simulatedValues[2] = (int)(Math.random() * 1000); // Disk KB/s
        simulatedValues[3] = (int)(Math.random() * 500); // Network KB/s
        simulatedValues[4] = (int)(30 + Math.random() * 40); // Temperature °C
        simulatedValues[5] = (int)(50 + Math.random() * 200); // Power W

        updateSystemData(simulatedValues);
    }

    private void updateStatusPanel(int[] values) {
        statusPanel.removeAll();
        String[] units = {"%", "MB", "KB/s", "KB/s", "°C", "W"};

        for (int i = 0; i < 6; i++) {
            statusPanel.add(new JLabel(chartNames[i] + ":"));
            JLabel valueLabel = new JLabel(values[i] + " " + units[i]);
            valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD));
            statusPanel.add(valueLabel);
        }
        statusPanel.revalidate();
    }

    private void updateInfoArea(long runtime, int updateCount) {
        infoArea.setText("System Monitoring Dashboard\n" +
                "Runtime: " + runtime + " seconds\n" +
                "Updates: " + updateCount + "\n" +
                "Status: Running\n" +
                "Update Interval: 1 second\n\n" +
                "Monitoring 6 parameters:\n" +
                "- CPU Usage (%)\n" +
                "- Memory Usage (MB)\n" +
                "- Disk I/O (KB/s)\n" +
                "- Network (KB/s)\n" +
                "- Temperature (°C)\n" +
                "- Power Consumption (W)\n\n" +
                "Data points stored: " +
                (chartData.isEmpty() ? 0 : chartData.get(0).size()) + "/" + MAX_DATA_POINTS);
    }

    private static class ChartPanel extends JPanel {
        private String title;
        private List<Integer> data;
        private Color lineColor;

        public ChartPanel(String title) {
            this.title = title;
            this.data = new ArrayList<>();
            this.lineColor = new Color((int)(Math.random() * 255),
                    (int)(Math.random() * 255),
                    (int)(Math.random() * 255));
            setBorder(BorderFactory.createTitledBorder(title));
            setBackground(Color.WHITE);
        }

        public void updateData(List<Integer> newData) {
            this.data = new ArrayList<>(newData);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (data.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 20;
            int height = getHeight() - 40;
            int offsetX = 10;
            int offsetY = 20;

            int minVal = data.stream().min(Integer::compare).orElse(0);
            int maxVal = data.stream().max(Integer::compare).orElse(100);
            if (minVal == maxVal) maxVal = minVal + 1; // Avoid division by zero

            g2d.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i <= 10; i++) {
                int y = offsetY + (height * i / 10);
                g2d.drawLine(offsetX, y, offsetX + width, y);
            }

            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(2.0f));

            if (data.size() > 1) {
                for (int i = 0; i < data.size() - 1; i++) {
                    int x1 = offsetX + (width * i / Math.max(data.size() - 1, 1));
                    int x2 = offsetX + (width * (i + 1) / Math.max(data.size() - 1, 1));

                    int y1 = offsetY + height - (height * (data.get(i) - minVal) / (maxVal - minVal));
                    int y2 = offsetY + height - (height * (data.get(i + 1) - minVal) / (maxVal - minVal));

                    g2d.drawLine(x1, y1, x2, y2);
                }
            }

            g2d.setColor(Color.BLACK);
            if (!data.isEmpty()) {
                String valueText = "Current: " + data.getLast();
                g2d.drawString(valueText, offsetX, height + offsetY + 15);
            }

            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (Exception _) {}

            new MonitoringDashboard().setVisible(true);
        });
    }
}
