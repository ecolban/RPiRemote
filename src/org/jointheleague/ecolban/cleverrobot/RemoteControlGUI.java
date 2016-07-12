package org.jointheleague.ecolban.cleverrobot;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class RemoteControlGUI implements Runnable, KeyListener, ActionListener {

    private int[] speeds = new int[2];
    private boolean leftUp;
    private boolean leftDown;
    private boolean rightUp;
    private boolean rightDown;
    private Timer ticker = new Timer(10, this);
    private JPanel speedPanel;
    private BoundedRangeModel leftSpeedModel = new DefaultBoundedRangeModel(0, 0, -500, 500);
    private BoundedRangeModel rightSpeedModel = new DefaultBoundedRangeModel(0, 0, -500, 500);
    private boolean stopRobot;

    public static void main(String[] args) {
        RemoteControlGUI remoteControl = new RemoteControlGUI();
        SwingUtilities.invokeLater(remoteControl);
        remoteControl.connectToRobot();
    }

    @Override
    public void run() {
        JFrame frame = new JFrame("CleverRobot");
        frame.setLayout(new BorderLayout());
        final JComponent leftSlider = buildSpeedControl(0, leftSpeedModel);
        final JComponent rightSlider = buildSpeedControl(1, rightSpeedModel);
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 100, 0));
        sliderPanel.add(leftSlider);
        sliderPanel.add(rightSlider);
        frame.addKeyListener(this);
        frame.setFocusable(true);
        frame.add(sliderPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        ticker.start();
        
    }

    private void connectToRobot() {
        try (Socket robotSocket = new Socket("rpi", 3333);
                PrintWriter out = new PrintWriter(robotSocket.getOutputStream(), true);) {
                boolean running = true;
                int ls = 0, rs = 0;
                while(running ) {
                    Thread.sleep(200);
                    if (speeds[0] != ls || speeds[1] != rs) {
                        ls = speeds[0];
                        rs = speeds[1];
                        out.format("driveDirect(%d, %d)\n", ls, rs);
                    }
                }
        } catch (IOException | InterruptedException e) {
        }

    }

    private JComponent buildSpeedControl(final int side, BoundedRangeModel model) {
        speedPanel = new JPanel();
        speedPanel.setLayout(new BoxLayout(speedPanel, BoxLayout.Y_AXIS));
        final JSlider slider = new JSlider(model);
        slider.setOrientation(JSlider.VERTICAL);
        speedPanel.add(slider);
        slider.setMajorTickSpacing(100);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        final JLabel speedLabel = new JLabel("0");
        speedPanel.add(speedLabel);
        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (!slider.getValueIsAdjusting()) {
                    int speed = slider.getValue();
                    speedLabel.setText("" + speed);
                    speeds[side] = speed;
                }
            }
        });

        return speedPanel;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_W:
            leftUp = true;
            break;
        case KeyEvent.VK_S:
            leftDown = true;
            break;
        case KeyEvent.VK_O:
            rightUp = true;
            break;
        case KeyEvent.VK_L:
            rightDown = true;
            break;
        default:

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_W:
            leftUp = false;
            break;
        case KeyEvent.VK_S:
            leftDown = false;
            break;
        case KeyEvent.VK_O:
            rightUp = false;
            break;
        case KeyEvent.VK_L:
            rightDown = false;
            break;
        case KeyEvent.VK_SPACE:
            stopRobot = true;
        default:

        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int acceleration = 3;
        int deceleration = 100;
        if (stopRobot) {
            speeds[0] = speeds[1] = 0;
            stopRobot = false;
        } else if (leftUp & !leftDown) {
            speeds[0] = Math.min(500, speeds[0] + acceleration);
        } else if (!leftUp && leftDown) {
            speeds[0] = Math.max(-500, speeds[0] - acceleration);
        } else if (speeds[0] > 0) {
            speeds[0] -= Math.min(deceleration, speeds[0]);
        } else if (speeds[0] < 0) {
            speeds[0] += Math.min(deceleration, -speeds[0]);
        }
        leftSpeedModel.setValue(speeds[0]);
        if (rightUp & !rightDown) {
            speeds[1] = Math.min(500, speeds[1] + acceleration);
        } else if (!rightUp && rightDown) {
            speeds[1] = Math.max(-500, speeds[1] - acceleration);
        } else if (speeds[1] > 0) {
            speeds[1] -= Math.min(deceleration, speeds[1]);
        } else if (speeds[1] < 0) {
            speeds[1] += Math.min(deceleration, -speeds[1]);
        }
        rightSpeedModel.setValue(speeds[1]);

        speedPanel.repaint();

    }

}
