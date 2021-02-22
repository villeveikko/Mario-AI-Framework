package engine.chat;

import javax.swing.*;

import engine.core.MarioGame;
import engine.helper.Assets;
import engine.helper.MarioActions;
import engine.helper.EventType;
import engine.helper.SpriteType;

import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MarioChat extends JComponent {
	public MarioChatWorker chatWorker;
	
	private GraphicsConfiguration graphicsConfiguration;
	
	private static final int chatWindowWidth = 400;
	
	private javax.swing.JScrollPane scrollPane;
	private static javax.swing.JTextArea textArea;
	public javax.swing.JTextField txtInput;
	
	private MarioGame game;
	
	public MarioChat(MarioGame game, float scale) {
		this.game = game;
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
	
	public void addMessageFromAgent(String message) {
		this.addMessage("Mario: " + message);
	}
	
	private void txtInputActionPerformed(java.awt.event.ActionEvent evt) {                                         
		var newMessage = txtInput.getText();
		if(newMessage.isEmpty()) {
			return;
		}
		this.addMessage("User: " + newMessage);
		this.parseUserMessageToCommand(newMessage);
		txtInput.setText("");
	}
	
	private void addMessage(String message) {
		textArea.append("\n" + message);
		this.validate();
		JScrollBar vertical = scrollPane.getVerticalScrollBar();
		vertical.setValue( vertical.getMaximum() );
	}
	
	private void parseUserMessageToCommand(String message) {
		/**
		 * TODO: do some actual parsing of the text and determine if it is a command or not.
		 * Currently this method only deals with simple commands
		 */
		switch(message.toLowerCase()) {
			case "stop":
				this.game.newAgent = new agents.doNothing.Agent();
				this.addMessageFromAgent("Okie dokie.");
				break;
			case "start":
				this.game.newAgent = new agents.sergeyPolikarpov.Agent();
				this.addMessageFromAgent("It's-a-me, Mario!");
				break;
			case "speedrun":
				this.game.newAgent = new agents.robinBaumgarten.Agent();
				this.addMessageFromAgent("Let's-a-go!");
				break;
			case "user":
				this.game.newAgent = new agents.human.Agent();
				txtInput.setFocusable(false); //TODO: How to return focus??
				break;
			//Hard-coded command(s) to make debugging easier
			//TODO
			//Currently things break when attempting to restart
			case "reset": 
			case "restart":
				//this.addMessage("User: " + message);
				//txtInput.setText("");
				//this.chatWorker.interrupt();
				//MarioGame newGame = new MarioGame();
				//newGame.runGame(new agents.doNothing.Agent(), getLevel("../levels/original/lvl-1.txt"), 200, 0, true, false);
				//this.game.runGame(new agents.doNothing.Agent(), getLevel("../levels/original/lvl-1.txt"), 200, 0, true, true);
				//this.game.window.dispose();
				break;
		}
	}
	
	/*
	public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
        }
        return content;
    }
	*/
}