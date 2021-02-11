package engine.chat;

import engine.helper.Assets;
import engine.helper.MarioActions;
import engine.helper.EventType;

import java.util.ArrayList;

public class MarioChatMessage {
	public EventType type;
	public String message;
	
	public MarioChatMessage(EventType type, String message) {
		this.type = type;
		this.message = message;
	}
	
	@Override
    public boolean equals(Object obj) {
        MarioChatMessage otherMessage = (MarioChatMessage) obj;
        return this.type.getValue() == otherMessage.type.getValue();
    }
}