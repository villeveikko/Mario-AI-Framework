package engine.core;

import engine.helper.Assets;
import engine.helper.MarioActions;
import engine.helper.EventType;
import engine.helper.SpriteType;

import java.lang.Thread;
import java.util.ArrayList;

public class MarioChatWorker extends Thread {
	//Static variables
	private static final int funnelRefreshInterval = 500; //ms
	//Funnel components
	private ArrayList<MarioEvent> marioEvents = new ArrayList<MarioEvent>();
	private ArrayList<MarioAgentEvent> marioAgentEvents = new ArrayList<MarioAgentEvent>();
	//Reference to chat using the worker
	private MarioChat marioChat;

	//Constructor
	public MarioChatWorker(MarioChat marioChat) {
		this.marioChat = marioChat;
	}

	//Call this whenever new events come from the game loop
	public void AddNewEventsToFunnel(ArrayList<MarioEvent> lastMarioEvents, MarioAgentEvent marioAgentEvent) {
		//TODO: Add possibility for a message be sent instantly, if it is important enough
		this.marioEvents.addAll(lastMarioEvents);
		this.marioAgentEvents.add(marioAgentEvent);
	}
	
	public void run() {
		while(true) {
			//1) Determine what message(s) to send
			var eventMessages = this.TransformMarioEventsToMessages();
			var agentMessages = this.TransformMarioAgentEventsToMessages();
			//2) Send those messages with this.marioChat.addMessageFromAgent(message)
			for(String m : eventMessages) {
				this.marioChat.addMessageFromAgent(m);
			}
			for(String m : agentMessages) {
				this.marioChat.addMessageFromAgent(m);
			}
			//3) Clear the funnel
			this.marioEvents = new ArrayList<MarioEvent>();
			this.marioAgentEvents = new ArrayList<MarioAgentEvent>();
			//4) Pause
			try {
				Thread.sleep(funnelRefreshInterval);
			} catch(InterruptedException e) {
				//No need to do anything?
			}
			
		}
	}
	
	private ArrayList<String> TransformMarioEventsToMessages() {
		var result = new ArrayList<String>();
		for(MarioEvent e : this.marioEvents) {
			String message = null;
            if (e.getEventType() == EventType.COLLECT.getValue()) {
                if (e.getEventParam() == SpriteType.FIRE_FLOWER.getValue()) {
                    message = "Got a fire flower!";
                }
                if (e.getEventParam() == SpriteType.MUSHROOM.getValue()) {
                    message = "Got a mushroom!";
                }
            }
            if (e.getEventType() == EventType.BUMP.getValue() && e.getEventParam() == 22 //OBS_BRICK
                    && e.getMarioState() > 0) {
                message = "Whoops!";
            }
			if(message != null) {
				boolean isDuplicate = false;
				for(String s : result) {
					if(s.equals(message)) {
						isDuplicate = true;
						break;
					}
				}
				if(isDuplicate) {
					continue;
				}
				result.add(message);
			}
        }
		return result;
	} 
	
	private ArrayList<String> TransformMarioAgentEventsToMessages() {
		var result = new ArrayList<String>();
		for(MarioAgentEvent e : this.marioAgentEvents) {
			String message = null;
            var lastActions = e.getActions();
			if(lastActions[MarioActions.JUMP.getValue()]) {
				message = "Jump!";
			}
			if(message != null) {
				boolean isDuplicate = false;
				for(String s : result) {
					if(s.equals(message)) {
						isDuplicate = true;
						break;
					}
				}
				if(isDuplicate) {
					continue;
				}
				result.add(message);
			}
        }
		return result;		
	} 
}