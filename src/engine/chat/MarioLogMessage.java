package engine.chat;

import engine.helper.EventType;
import java.time.LocalTime;

public class MarioLogMessage {
	public EventType type;
	public String message;
	public LocalTime timeStamp;
	
	public MarioLogMessage(EventType type, String message, LocalTime timeStamp) {
		this.type = type;
		this.message = message;
		this.timeStamp = timeStamp;
	}
}