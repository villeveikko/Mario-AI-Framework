package engine.chat;

import engine.core.MarioEvent;
import engine.core.MarioAgentEvent;
import engine.core.MarioForwardModel;
import engine.helper.Assets;
import engine.helper.MarioActions;
import engine.helper.EventType;
import engine.helper.SpriteType;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.Random;

/**
 * Handles the transforming of the event data received from the game into proper sentences, and sends them 
 * to the chat.
 * Also makes sure that Mario doesn't spam too much.
 */
public class MarioChatWorker extends Thread {
	//Static variables
	private static final int funnelRefreshInterval = 2000; //ms
	private static final int funnelCheckupInterval = 50; //ms
	//Funnel components
	private ArrayList<MarioChatMessage> recentMessages = new ArrayList<MarioChatMessage>();
	//Reference to chat
	private MarioChat marioChat;
	//Arrays for randomized messages
	private static final String[] JumpSounds = {
		"Yahhoo!",
		"Wahoo",
		"Hop!"
	};
	private static final String[] StompSounds = {
		"Coming through!",
		"Sorry about that, *!",
		"Make way, *!"
	};

	//Constructor
	public MarioChatWorker(MarioChat marioChat) {
		this.marioChat = marioChat;
	}

	/**
	 * Parses out a message from the game events, then sends it to the chat if a message of the same type 
	 * has not been sent recently
	 */
	public void AddNewEventsToFunnel(ArrayList<MarioEvent> lastMarioEvents, MarioAgentEvent marioAgentEvent) {
		var marioEventMessages = this.TransformMarioEventsToMessages(lastMarioEvents);
		var marioAgentMessages = this.TransformMarioAgentEventToMessages(marioAgentEvent);
		for(MarioChatMessage m : marioEventMessages) {
			if(IsMessageDuplicate(m, recentMessages)) {
				continue;
			}
			this.marioChat.addMessageFromAgent(m.message);
			recentMessages.add(m);
		}
		for(MarioChatMessage m : marioAgentMessages) {
			if(IsMessageDuplicate(m, recentMessages)) {
				continue;
			}
			this.marioChat.addMessageFromAgent(m.message);
			recentMessages.add(m);
		}
	}
	
	public void run() {
		while(true) {
			if(recentMessages.size() > 0) {
				try {
					Thread.sleep(funnelRefreshInterval);
					recentMessages = new ArrayList<MarioChatMessage>();
				} catch(InterruptedException e) {
					//No need to do anything?
					recentMessages = new ArrayList<MarioChatMessage>();
				}
			} else {
				try {
					Thread.sleep(funnelCheckupInterval);
				} catch(InterruptedException e) {
					//No need to do anything?
				}
			}
		}
	}
	
	private ArrayList<MarioChatMessage> TransformMarioEventsToMessages(ArrayList<MarioEvent> marioEvents) {
		var result = new ArrayList<MarioChatMessage>();
		for(MarioEvent e : marioEvents) {
			String message = null;
			EventType type = null;
			/* Legend for the event types:
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
			*/
			switch(e.getEventType()) {
				case 1: // bump
					switch(e.getEventParam()) {
						case MarioForwardModel.OBS_BRICK:
							message = e.getMarioState() > 0 ? "Whoops!" : "Ouch!";
							type = EventType.BUMP;
							break;
					}
					break;
				case 2: // stomp kill
					String target = "";
					switch(e.getEventParam()) {
						case MarioForwardModel.OBS_GOOMBA:
						case MarioForwardModel.OBS_GOOMBA_WINGED:
							target = "Goomba";
							break;
						case MarioForwardModel.OBS_RED_KOOPA:
						case MarioForwardModel.OBS_RED_KOOPA_WINGED:
						case MarioForwardModel.OBS_GREEN_KOOPA:
						case MarioForwardModel.OBS_GREEN_KOOPA_WINGED:
							target = "Koopa";
							break;
					}
					message = GetRandomMessage(StompSounds, target);
					type = EventType.STOMP_KILL;
					break;
				case 3: // fire kill
					message = "Roasted!";
					type = EventType.FIRE_KILL;
					break;
				case 4: // shell kill
					message = "Strike!";
					type = EventType.SHELL_KILL;
					break;
				case 8: // collect
					if (e.getEventParam() == SpriteType.FIRE_FLOWER.getValue()) {
						message = "Got a fire flower!";
					}
					if (e.getEventParam() == SpriteType.MUSHROOM.getValue()) {
						message = "Got a mushroom!";
					}
					type = EventType.COLLECT;
					break;
				case 9: // hurt
					message = "Ouch!!";
					type = EventType.HURT;
					break;
				case 11: // lose
					message = "Mamma mia!";
					type = EventType.LOSE;
					break;
			}
			if(message != null) {
				result.add(new MarioChatMessage(type, message));
			}
        }
		return result;
	}
	
	private ArrayList<MarioChatMessage> TransformMarioAgentEventToMessages(MarioAgentEvent e) {
		var result = new ArrayList<MarioChatMessage>();
		var lastActions = e.getActions();
		if(lastActions[MarioActions.JUMP.getValue()]) {
			result.add(new MarioChatMessage(EventType.JUMP, GetRandomMessage(JumpSounds, "")));
		}
		return result;		
	}
	
	private static boolean IsMessageDuplicate(MarioChatMessage message, ArrayList<MarioChatMessage> otherMessages) {
		boolean isDuplicate = false;
		for(MarioChatMessage m : otherMessages) {
			if(m.equals(message)) {
				isDuplicate = true;
				break;
			}
		}
		return isDuplicate;
	}
	
	/**
	 * Gets a random string from a given array, and replaces all the asterisks in the selected string with
	 * a given replacement
	 * 
	 * @param messageArray		The array where a random message should be selected
	 * @param replacement		A string which will replace all the * symbols in the randomly selected string
	 *
	 * @return A random string from the given array, with all its asterisks replaced with given string
	 */
	private static String GetRandomMessage(String[] messageArray, String replacement) {
		Random rand = new Random();
		int index = rand.nextInt(messageArray.length);
		String result = messageArray[index].replace("*", replacement);
		return result;
	}
}