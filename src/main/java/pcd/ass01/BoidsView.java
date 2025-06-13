package pcd.ass01;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import pcd.ass01.actors.BoidExchangeProtocol;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import static pcd.ass01.SimulatorExchangeProtocol.*;

public class BoidsView implements ChangeListener, ActionListener {
	private final String START = "Start";
	private final String STOP = "Stop";
	private final String PAUSE = "Pause";
	private final String RESUME = "Resume";

	private final JFrame frame;
	private final BoidsPanel boidsPanel;
	private final JSlider cohesionSlider, separationSlider, alignmentSlider;
	private final JButton startStopButton, pauseResumeButton;
	private final JTextField boidsCountField;
	private final BoidsModel model;
	private final int width, height;
	private final ActorRef simulator;

	public BoidsView(BoidsModel model, ActorRef simulator, int width, int height) {
		this.model = model;
		this.width = width;
		this.height = height;
		this.simulator = simulator;

		frame = new JFrame("Boids Simulation");
		frame.setSize(width, height);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		JPanel cp = new JPanel();
		LayoutManager layout = new BorderLayout();
		cp.setLayout(layout);
		boidsPanel = new BoidsPanel(this, model);
		cp.add(BorderLayout.CENTER, boidsPanel);
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BorderLayout());

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(new JLabel("Number of Boids:"));
		boidsCountField = new JTextField(5);
		boidsCountField.setText("2500");
		buttonsPanel.add(boidsCountField);
		startStopButton = new JButton(START);
		pauseResumeButton = new JButton(PAUSE);
		startStopButton.setEnabled(true);
		pauseResumeButton.setEnabled(false);
		startStopButton.addActionListener(this);
		pauseResumeButton.addActionListener(this);
		buttonsPanel.add(startStopButton);
		buttonsPanel.add(pauseResumeButton);

		JPanel slidersPanel = new JPanel();
		slidersPanel.setLayout(new GridLayout(3, 2));

		cohesionSlider = makeSlider();
		separationSlider = makeSlider();
		alignmentSlider = makeSlider();

		slidersPanel.add(new JLabel("Separation"));
		slidersPanel.add(separationSlider);
		slidersPanel.add(new JLabel("Alignment"));
		slidersPanel.add(alignmentSlider);
		slidersPanel.add(new JLabel("Cohesion"));
		slidersPanel.add(cohesionSlider);

		controlPanel.add(BorderLayout.CENTER, slidersPanel);
		controlPanel.add(BorderLayout.SOUTH, buttonsPanel);

		cp.add(BorderLayout.SOUTH, controlPanel);
		frame.setContentPane(cp);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				if (JOptionPane.showConfirmDialog(frame,
						"Are you sure you want to close this window?", "Close Window?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
					simulator.tell(new StopSimulatorMsg(), ActorRef.noSender());
					System.exit(0);
				}
			}
		});

		frame.setVisible(true);
	}

	private JSlider makeSlider() {
		var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
		slider.setMajorTickSpacing(10);
		slider.setMinorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
		labelTable.put(0, new JLabel("0"));
		labelTable.put(10, new JLabel("1"));
		labelTable.put(20, new JLabel("2"));
		slider.setLabelTable(labelTable);
		slider.setPaintLabels(true);
		slider.addChangeListener(this);
		return slider;
	}

	public void update(int frameRate, List<Boid> boids) {
		boidsPanel.setBoids(boids);
		boidsPanel.setFrameRate(frameRate);
		boidsPanel.repaint();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == separationSlider) {
			var val = separationSlider.getValue();
			model.setSeparationWeight(0.1*val);
		} else if (e.getSource() == cohesionSlider) {
			var val = cohesionSlider.getValue();
			model.setCohesionWeight(0.1*val);
		} else {
			var val = alignmentSlider.getValue();
			model.setAlignmentWeight(0.1*val);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == startStopButton) {
			if (startStopButton.getText() == START) {
				boidsCountField.setEnabled(false);
				getNumBoids().ifPresent(val -> {
					System.out.println("Boids count: " + val);
					simulator.tell(new StartSimulationMsg(val), ActorRef.noSender());
					startStopButton.setText(STOP);
					pauseResumeButton.setEnabled(true);
				});
			} else {
				boidsCountField.setEnabled(true);
				simulator.tell(new StopSimulationMsg(), ActorRef.noSender());
				startStopButton.setText(START);
				pauseResumeButton.setText(PAUSE);
				pauseResumeButton.setEnabled(false);
			}
		}
		if (e.getSource() == pauseResumeButton) {
			if (pauseResumeButton.getText() == PAUSE) {
				simulator.tell(new PauseSimulationMsg(), ActorRef.noSender());
				pauseResumeButton.setText(RESUME);
			} else {
				simulator.tell(new ResumeSimulationMsg(), ActorRef.noSender());
				pauseResumeButton.setText(PAUSE);
			}
		}
	}

	private Optional<Integer> getNumBoids() {
		Optional<Integer> result = Optional.empty();
		try {
			result = Optional.of(Integer.parseInt(boidsCountField.getText()));
			if (result.get() < 0) {
				JOptionPane.showMessageDialog(frame,
						"Please enter a positive number of boids.",
						"Invalid Input", JOptionPane.ERROR_MESSAGE);
				result = Optional.empty();
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame,
					"Please enter a valid number.",
					"Invalid Input", JOptionPane.ERROR_MESSAGE);
		}
		return result;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}