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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.DefaultCaret;

public class MarioChat extends JComponent {	
	private GraphicsConfiguration graphicsConfiguration;

	private MarioLogMessage lastMessage = null;
	
	private JScrollPane scrollPane;
	private JTextPane tPane;
	public JTextField txtInput;
	
	private MarioGame game;
	public MarioChatWorker chatWorker;
	
	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final int chatWindowWidth = 400;
	private static final Pattern dateTimePattern = Pattern.compile("\\d\\d:\\d\\d:\\d\\d");
	
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
		scrollPane = new JScrollPane();
		txtInput = new JTextField();
        tPane = new JTextPane();                
		DefaultCaret caret = (DefaultCaret)tPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		scrollPane.setViewportView(tPane);
		
		this.addMessage("Welcome to chat with Mario. The following commands are available:\n" +
		"start\n" +
		"stop\n" +
		"speedrun\n" +
		"user\n" +
		"why did you <action>?\n" +
		"earlier\n" +
		"later\n" +
		"at <HH:mm:ss>\n"
		, Color.GRAY);
		
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
		this.addMessage("Mario: " + message, Color.BLUE);
	}
	
	private void txtInputActionPerformed(java.awt.event.ActionEvent evt) {                                         
		var newMessage = txtInput.getText();
		if(newMessage.isEmpty()) {
			return;
		}
		this.addMessage("User: " + newMessage, Color.BLACK);
		this.parseUserMessageToCommand(newMessage);
		txtInput.setText("");
	}
	
	private void addMessageWithTimeStamp(String message, Color color) {
		var timeStamp = java.time.LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		var fullMessage = "\n" + timeStamp.format(formatter) + " - " + message;
		this.addMessage(fullMessage, color);
	}
	
	//TODO: Change most addMessage calls to addMessageWithTimeStamp. Remove the time stamp insert from addMessage
	private void addMessage(String message, Color color) {
		var timeStamp = java.time.LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		var fullMessage = "\n" + timeStamp.format(formatter) + " - " + message;
		
		StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_LEFT);

        int len = tPane.getDocument().getLength();
        tPane.setCaretPosition(len);
        tPane.setCharacterAttributes(aset, false);
        tPane.replaceSelection(fullMessage);
	}
	
	private void parseUserMessageToCommand(String message) {
		/**
		 * TODO: do some actual parsing of the text and determine if it is a command or not.
		 * Currently this method only deals with simple commands
		 */
		switch(CleanString(message)) {
			case "stop":
				this.game.newAgent = new agents.cautiousIdle.Agent();
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
		if(message.toLowerCase().contains("why did you ")) {
			var action = message.toLowerCase().substring(12);
			var timeStampIndex = action.indexOf(" at ");
			LocalTime timeStamp = null;
			if(timeStampIndex > -1) {
				var timeStampString = action.substring(timeStampIndex + 4, timeStampIndex + 4 + 8);
				timeStamp = LocalTime.parse(timeStampString, dtf);
				action = action.substring(0, timeStampIndex);
			}
			var eventType = StringToEventType(CleanString(action));
			if(eventType == null) {
				this.addMessageFromAgent("Why did I what?");
			} else {
				this.lastMessage = this.chatWorker.CheckHistoryForEventType(eventType, timeStamp);
				this.addMessageFromAgent(this.lastMessage.message);
			}
		}
		if(this.lastMessage != null) {
			if(message.toLowerCase().contains("earlier")) {
				this.lastMessage = this.chatWorker.CheckEarlierHistoryForEventType(this.lastMessage.type, this.lastMessage.timeStamp);
				this.addMessageFromAgent(this.lastMessage.message);
			}
			if(message.toLowerCase().contains("later")) {
				this.lastMessage = this.chatWorker.CheckLaterHistoryForEventType(this.lastMessage.type, this.lastMessage.timeStamp);
				this.addMessageFromAgent(this.lastMessage.message);
			}
			Matcher m = dateTimePattern.matcher(message);
			if(m.matches()) {
				var timeStampString = m.group();
				System.out.println(timeStampString);
			}
		}
	}
	
	private static String CleanString(String s) {
		var result = s.replaceAll("[^a-zA-Z\\s]", "");
		result = result.trim();
		return result.toLowerCase();
	}
	
	private static EventType StringToEventType(String s) {
		// TODO: Add synonyms
		switch(s) {
			case "bump":
				return EventType.BUMP;
			case "stomp":
				return EventType.STOMP_KILL;
			case "roast":
				return EventType.FIRE_KILL;
			case "shell":
				return EventType.SHELL_KILL;
			case "fall":
				return EventType.FALL_KILL;
			case "jump":
			case "jump up":
				return EventType.JUMP;
			case "land":
				return EventType.LAND;
			case "collect":
				return EventType.COLLECT;
			case "get hurt":
				return EventType.HURT;
			case "kick":
				return EventType.KICK;
			case "lose":
				return EventType.LOSE;
			case "win":
				return EventType.WIN;
			default:
				return null;
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