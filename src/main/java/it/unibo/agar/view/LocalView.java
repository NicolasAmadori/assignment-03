package it.unibo.agar.view;

import it.unibo.agar.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.*;

public class LocalView extends JFrame {

    private static final double SENSITIVITY = 2;
    private final GamePanel gamePanel;
    private final DistributedGameStateManager gameStateManager;
    private final String playerId;
    private final PlayerController playerControllerStub;

    public LocalView(DistributedGameStateManager gameStateManager, String playerId, PlayerController playerControllerStub) {
        this.gameStateManager = gameStateManager;
        this.playerId = playerId;
        this.playerControllerStub = playerControllerStub;

        setTitle("Agar.io - Local View (" + playerId + ")");
        setPreferredSize(new Dimension(600, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        gamePanel = new GamePanel(gameStateManager, playerId);
        add(gamePanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    playerControllerStub.terminate(true);
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        setupMouseControls();
        pack();
        setLocationRelativeTo(null); // Center on screen
        setFocusable(true);
        requestFocusInWindow();
        setVisible(true);
    }

    private void setupMouseControls() {
        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Optional<Player> playerOpt = gameStateManager.getWorld().getPlayerById(playerId);
                if (playerOpt.isPresent()) {
                    Point mousePos = e.getPoint();
                    // Player is always in the center of the local view
                    double viewCenterX = gamePanel.getWidth() / 2.0;
                    double viewCenterY = gamePanel.getHeight() / 2.0;

                    double dx = mousePos.x - viewCenterX;
                    double dy = mousePos.y - viewCenterY;

                    // Normalize the direction vector
                    double magnitude = Math.hypot(dx, dy);
                    if (magnitude > 0) { // Avoid division by zero if mouse is exactly at center
                        gameStateManager.movePlayerDirection((dx / magnitude) * SENSITIVITY, (dy / magnitude) * SENSITIVITY);
                    } else {
                        gameStateManager.movePlayerDirection(0, 0); // Stop if mouse is at center
                    }
                    // Repainting is handled by the main game loop timer
                }
            }
        });
    }

    public void repaintView() {
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    public void showView() {
        SwingUtilities.invokeLater(() -> {
            setLocationRelativeTo(null);
            setVisible(true);
            toFront();
            requestFocusInWindow();
        });
    }

    public void closeView() {
        SwingUtilities.invokeLater(this::dispose);
    }

    public void showMessage(String msg) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, msg, "Game Ended", JOptionPane.INFORMATION_MESSAGE)
        );
    }
}
