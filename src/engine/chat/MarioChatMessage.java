package engine.chat;

import engine.core.MarioForwardModel;
import engine.helper.EventType;

import java.util.ArrayList;

public class MarioChatMessage {
	public EventType type;
	public String message;
	public MarioForwardModel model;
	
	public MarioChatMessage(EventType type, String message, MarioForwardModel model) {
		this.type = type;
		this.message = message;
		this.model = model;
	}
	
	@Override
    public boolean equals(Object obj) {
        MarioChatMessage otherMessage = (MarioChatMessage) obj;
        return this.type.getValue() == otherMessage.type.getValue();
    }
}