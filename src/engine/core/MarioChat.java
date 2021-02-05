package engine.core;

import javax.swing.*;

import engine.helper.Assets;
import engine.helper.MarioActions;
import engine.helper.EventType;
import engine.helper.SpriteType;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;


public class MarioChat extends JComponent {
	public MarioChatWorker chatWorker;
	
	private GraphicsConfiguration graphicsConfiguration;
	
	private static final int chatWindowWidth = 400;
	
	private javax.swing.JScrollPane scrollPane;
	private static javax.swing.JTextArea textArea;
	private javax.swing.JTextField txtInput;
	
	public MarioChat(float scale) {
		this.setEnabled(true);
		
		Dimension size = new Dimension((int) chatWindowWidth, (int) (240 * scale));
		setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
		
		setFocusable(true);
	}
	
	public void init() {
		graphicsConfiguration = getGraphicsConfiguration();
        Assets.init(graphicsConfiguration);
		
		//Create chat graphics
		scrollPane = new javax.swing.JScrollPane();
		textArea = new javax.swing.JTextArea();
		txtInput = new javax.swing.JTextField();
	
		textArea.setEditable(false);
		textArea.setColumns(0);
		textArea.setRows(0);
		textArea.setText("Welcome to the Chat Room with Mario. Say hi!\n");
		textArea.setWrapStyleWord(true);
		textArea.setCaretPosition(textArea.getDocument().getLength());
		scrollPane.setViewportView(textArea);
		txtInput.setToolTipText("Type anything...");
		txtInput.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				txtInputActionPerformed(evt);
			}
		});
				
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addContainerGap()
				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
					.addComponent(scrollPane)
					.addComponent(txtInput))
				.addContainerGap())
		);
		layout.setVerticalGroup(
			layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
			.addGroup(layout.createSequentialGroup()
				.addComponent(scrollPane)
				.addGap(5, 5, 5)
				.addComponent(txtInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
				.addContainerGap())
		);
		
		this.chatWorker = new MarioChatWorker(this);
		this.chatWorker.start();
	}
	
	public void updateEventInformation(ArrayList<MarioEvent> lastMarioEvents, MarioAgentEvent marioAgentEvent) {
		/*
		MarioEvents contain the following event types:
			BUMP(1),
			STOMP_KILL(2),
			FIRE_KILL(3),
			SHELL_KILL(4),
			FALL_KILL(5),
			JUMP(6),
			LAND(7),
			COLLECT(8),
			HURT(9),
			KICK(10),
			LOSE(11),
			WIN(12);
		They also contain an event parameter that can elaborate the event further...
		*/
		for(MarioEvent e : lastMarioEvents) {
            if (e.getEventType() == EventType.COLLECT.getValue()) {
                if (e.getEventParam() == SpriteType.FIRE_FLOWER.getValue()) {
                    this.addMessageFromAgent("Got a fire flower!");
                }
                if (e.getEventParam() == SpriteType.MUSHROOM.getValue()) {
                    this.addMessageFromAgent("Got a mushroom!");
                }
            }
            if (e.getEventType() == EventType.BUMP.getValue() && e.getEventParam() == 22 //OBS_BRICK
                    && e.getMarioState() > 0) {
                this.addMessageFromAgent("Whoops!");
            }
        }
		//MarioAgentEvent contains all the last actions done by the agent
		var lastActions = marioAgentEvent.getActions();
		if(lastActions[MarioActions.JUMP.getValue()]) {
			this.addMessageFromAgent("Jump!");
		} else {
			//this.addMessageFromAgent("I'm in the air!");
		}
	}
	
	public void addMessageFromAgent(String message) {
		this.addMessage("Mario: " + message);
	}
	
	private void txtInputActionPerformed(java.awt.event.ActionEvent evt) {                                         
		// TODO
		var newMessage = txtInput.getText();
		if(newMessage.isEmpty()) {
			return;
		}
		this.addMessage("User: " + newMessage);
		txtInput.setText("");
	}
	
	private void addMessage(String message) {
		textArea.append("\n" + message);
		this.validate();
		JScrollBar vertical = scrollPane.getVerticalScrollBar();
		vertical.setValue( vertical.getMaximum() );
	}
}