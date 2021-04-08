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
import java.util.TreeMap;
import java.util.Collections;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
	//Dictionary of all the past actions with context
	private TreeMap<LocalTime, ArrayList<MarioChatMessage>> messageHistory = new TreeMap<LocalTime, ArrayList<MarioChatMessage>>();
	//Arrays for randomized messages
	private static final String[] GenericJumpSounds = {
		"Yahhoo!",
		"Wahoo",
		"Hop!"
	};
	private static final String[] CautionSounds = {
		"That * is quite close...",
		"Better watch out for that *!",
		"Ugh, *s, am I right?"
	};
	private static final String[] CautionSoundsFlying = {
		"When did *s grow wings?",
		"Yikes, flying *s!"
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
	 * 
	 * @param lastMarioEvents		List of all MarioEvents that occurred in the last frame
	 * @param marioAgentEvent		Tells what the agent did last frame
	 * @param model					Used to determine blocks and sprites in vicinity
	 */
	public void AddNewEventsToFunnel(ArrayList<MarioEvent> lastMarioEvents, MarioAgentEvent marioAgentEvent, MarioForwardModel model) {
		var allMessages = new ArrayList<MarioChatMessage>();
		allMessages.addAll(this.TransformMarioEventsToMessages(lastMarioEvents, model));
		allMessages.addAll(this.TransformMarioAgentEventToMessages(marioAgentEvent, model));
		allMessages.addAll(this.TransformForwardModelToObservations(model));
		if(allMessages.size() == 0) {
			return;
		}
		var localTime = java.time.LocalTime.now();
		if(!messageHistory.containsKey(localTime)) {
			messageHistory.put(localTime, new ArrayList<MarioChatMessage>());
		}
		for(MarioChatMessage m : allMessages) {
			messageHistory.get(localTime).add(m); // Add "message" to history anyway; this can be important when checking history of actions
			if(IsMessageDuplicate(m, recentMessages)) {
				continue;
			}
			this.marioChat.addMessageFromAgent(m.message);
			recentMessages.add(m);
		}
		if(messageHistory.get(localTime).isEmpty()) {
			// This should not happen anymore, but doesn't hurt to check
			messageHistory.remove(localTime);
		}
	}
	
	/**
	 * Looks into a past message of a given EventType and gives a reasoning for it
	 *
	 * @param type		The EventType that is looked for
	 */
	public MarioLogMessage CheckHistoryForEventType(EventType type, LocalTime timeStamp) {
		if(messageHistory.isEmpty()) {
			return new MarioLogMessage(type, "Haven't done anything yet!", java.time.LocalTime.now());
		}
		var allKeys = new ArrayList<LocalTime>(messageHistory.keySet());
		//Reverse order so that the latest messages will be looked up first
		Collections.reverse(allKeys);
		if(timeStamp != null) {
			var higherKey = messageHistory.higherKey(timeStamp);
			/*var lowerKey = allKeys.lowerKey(timeStamp);
			var higherDifference = timeStamp.compareTo(higherKey);
			var lowerDifference = timeStamp.compareTo(lowerKey) * -1;
			if(higherDifference > lowerDifference) {
				
			}*/
			while(higherKey != null) {
				var messageList = messageHistory.get(higherKey);
				for(MarioChatMessage m : messageList) {
					if(m.type == type) {
						return new MarioLogMessage(type, this.GiveIntrospectionForEvent(m, higherKey), higherKey);
					}
				}
				higherKey = messageHistory.higherKey(higherKey);
			}
			return new MarioLogMessage(type, "I cannot find an event for that type with that time stamp.", java.time.LocalTime.now()); 
		}
		for(LocalTime key : allKeys) {
			var messageList = messageHistory.get(key);
			for(MarioChatMessage m : messageList) {
				if(m.type == type) {
					return new MarioLogMessage(type, this.GiveIntrospectionForEvent(m, key), key);
				}
			}
		}
		return new MarioLogMessage(type, "Have I done something like that?", java.time.LocalTime.now());
	}
	
	public MarioLogMessage CheckEarlierHistoryForEventType(EventType type, LocalTime lastEventTimeStamp) {
		if(messageHistory.isEmpty()) {
			return new MarioLogMessage(type, "Haven't done anything yet!", java.time.LocalTime.now());
		}
		var allKeys = new ArrayList<LocalTime>(messageHistory.keySet());
		//Reverse order so that the latest messages will be looked up first
		Collections.reverse(allKeys);
		for(LocalTime key : allKeys) {
			// If event is newer than the last one, skip
			if(key.compareTo(lastEventTimeStamp) >= 0) {
				continue;
			}
			var messageList = messageHistory.get(key);
			for(MarioChatMessage m : messageList) {
				if(m.type == type) {
					return new MarioLogMessage(type, this.GiveIntrospectionForEvent(m, key), key);
				}
			}
		}
		return new MarioLogMessage(type, "I don't recall jumping earlier...", java.time.LocalTime.now()); //TODO: replace "jumping" with event type
	}
	
	public MarioLogMessage CheckLaterHistoryForEventType(EventType type, LocalTime lastEventTimeStamp) {
		if(messageHistory.isEmpty()) {
			return new MarioLogMessage(type, "Haven't done anything yet!", java.time.LocalTime.now());
		}
		var allKeys = new ArrayList<LocalTime>(messageHistory.keySet());
		//No reverse order so that the earliest messages will be looked up first
		//Collections.reverse(allKeys);
		for(LocalTime key : allKeys) {
			// If event is older than the last one, skip
			if(key.compareTo(lastEventTimeStamp) <= 0) {
				continue;
			}
			var messageList = messageHistory.get(key);
			for(MarioChatMessage m : messageList) {
				if(m.type == type) {
					return new MarioLogMessage(type, this.GiveIntrospectionForEvent(m, key), key);
				}
			}
		}
		return new MarioLogMessage(type, "I don't recall jumping later...", java.time.LocalTime.now()); //TODO: replace "jumping" with event type
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
	
	private ArrayList<MarioChatMessage> TransformMarioEventsToMessages(ArrayList<MarioEvent> marioEvents, MarioForwardModel model) {
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
				case 6: // jump
					message = GetRandomMessage(GenericJumpSounds, "");
					type = EventType.JUMP;
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
				result.add(new MarioChatMessage(type, message, model));
			}
        }
		return result;
	}
	
	private ArrayList<MarioChatMessage> TransformMarioAgentEventToMessages(MarioAgentEvent e, MarioForwardModel model) {
		var result = new ArrayList<MarioChatMessage>();
		var lastActions = e.getActions();
		// No need for these events, for now
		return result;		
	}
	
	private ArrayList<MarioChatMessage> TransformForwardModelToObservations(MarioForwardModel model) {
		var result = new ArrayList<MarioChatMessage>();
		var completeObservation = model.getMarioCompleteObservation(0, 0);
		//Check for holes on both sides of Mario
		for(int i = completeObservation.length / 3; i < completeObservation.length / 1.3; i++) {
			var holeFound = true;
			for(int j = completeObservation[i].length / 2 + 1; j < completeObservation[i].length; j++) {
				if(completeObservation[i][j] != MarioForwardModel.OBS_NONE) {
					holeFound = false;
					break;
				}
			}
			if(holeFound) {
				result.add(new MarioChatMessage(EventType.CAUTION, GetRandomMessage(CautionSounds, "Hole"), model));
				break;
			}
		}
		//Check the surroundings quite close to Mario
		for(int i = completeObservation.length / 3; i < completeObservation.length / 1.3; i++) { //Outer array: left -> right?
			for(int j = completeObservation[i].length / 3; j < completeObservation[i].length / 1.3; j++) { //Inner array: up -> down?
				// BUG: Green koopa comes in with the value 2, and is registered as a goomba. Why?? 
				switch(completeObservation[i][j]) {
					case MarioForwardModel.OBS_GOOMBA:
						result.add(new MarioChatMessage(EventType.CAUTION, GetRandomMessage(CautionSounds, "Goomba"), model));
						break;
					case MarioForwardModel.OBS_GOOMBA_WINGED:
						result.add(new MarioChatMessage(EventType.CAUTION, GetRandomMessage(CautionSoundsFlying, "Goomba"), model));
						break;
					case MarioForwardModel.OBS_RED_KOOPA:
					case MarioForwardModel.OBS_GREEN_KOOPA:
						result.add(new MarioChatMessage(EventType.CAUTION, GetRandomMessage(CautionSounds, "Koopa"), model));
						break;
					case MarioForwardModel.OBS_RED_KOOPA_WINGED:
					case MarioForwardModel.OBS_GREEN_KOOPA_WINGED:
						result.add(new MarioChatMessage(EventType.CAUTION, GetRandomMessage(CautionSoundsFlying, "Koopa"), model));
						break;
				}
			}
		}
		return result;		
	}
	
	private String GiveIntrospectionForEvent(MarioChatMessage message, LocalTime timeStamp) {
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
		switch(message.type.getValue()) {
			case 6:
				var reason = this.CheckForDangersInFront(message.model, timeStamp);
				if(reason == null) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					return ("Did you mean the last time, around " + timeStamp.format(formatter) + "? I'm not sure, now that I think about it...");
				} else {
					return reason;
				}
			default:
				return null;
		}
	}
	
	private String CheckForDangersInFront(MarioForwardModel model, LocalTime timeStamp) {
		var dangers = new ArrayList<String>();
		var completeObservation = model.getMarioCompleteObservation(0, 0);
		//Check for holes in front of Mario
		for(int i = completeObservation.length / 2 + 1; i < completeObservation.length; i++) {
			var holeFound = true;
			for(int j = completeObservation[i].length / 2 + 1; j < completeObservation[i].length; j++) {
				if(completeObservation[i][j] != MarioForwardModel.OBS_NONE && completeObservation[i][j] != MarioForwardModel.OBS_COIN) {
					holeFound = false;
					break;
				}
			}
			if(holeFound) {
				dangers.add("a hole");
				break;
			}
		}
		//Check for enemies in front of Mario
		for(int i = completeObservation.length / 2; i < completeObservation.length / 1.3; i++) { // Check from Mario's position to the right
			for(int j = completeObservation[i].length / 3; j < completeObservation[i].length / 1.3; j++) { // Check from above and below Mario
				switch(completeObservation[i][j]) {
					case MarioForwardModel.OBS_GOOMBA:
						dangers.add("a goomba");
						break;
					case MarioForwardModel.OBS_GOOMBA_WINGED:
						dangers.add("a flying goomba");
						break;
					case MarioForwardModel.OBS_RED_KOOPA:
					case MarioForwardModel.OBS_GREEN_KOOPA:
						dangers.add("a koopa");
						break;
					case MarioForwardModel.OBS_RED_KOOPA_WINGED:
					case MarioForwardModel.OBS_GREEN_KOOPA_WINGED:
						dangers.add("a flying koopa");
						break;
				}
			}
		}
		if(dangers.size() == 0) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		var result = "Did you mean the last time, around " + timeStamp.format(formatter) + "? Probably because there was ";
		for(int i = 0; i < dangers.size(); i++) {
			result += dangers.get(i);
			if(i == dangers.size() - 1) {
				continue;
			} else if(i == dangers.size() - 2) {
				result += " and ";
			}
			result += ", ";
		}
		result += " in my way.";
		return result;
	}
	
	/**
	 * Checks if Mario is on the ground currently
	 * 
	 * @param model		The current forward model
	 *
	 * @return true if there is a walkable tile right below Mario, false otherwise.
	 */
	private static boolean IsMarioOnTheGround(MarioForwardModel model) {
		var sceneObservation = model.getMarioSceneObservation();
		var halfwayIndex = sceneObservation.length / 2;
		var tileBelowMario = sceneObservation[halfwayIndex][sceneObservation[halfwayIndex].length / 2 + 1];
		return tileBelowMario != MarioForwardModel.OBS_NONE && tileBelowMario != MarioForwardModel.OBS_COIN;
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