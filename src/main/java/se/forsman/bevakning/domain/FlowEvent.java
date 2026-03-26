package se.forsman.bevakning.domain;

import java.time.LocalDateTime;

public class FlowEvent {
    private final long piid;
    private final long ptid;
    private final int state;
    private final LocalDateTime started;
    private final LocalDateTime completed;
    private final String sender;
    private final String receiver;
    private final String msgType;

    public FlowEvent(long piid, long ptid, int state, LocalDateTime started, LocalDateTime completed,
                     String sender, String receiver, String msgType) {
        this.piid = piid;
        this.ptid = ptid;
        this.state = state;
        this.started = started;
        this.completed = completed;
        this.sender = sender;
        this.receiver = receiver;
        this.msgType = msgType;
    }

    public long getPiid() {
        return piid;
    }

    public long getPtid() {
        return ptid;
    }

    public int getState() {
        return state;
    }

    public LocalDateTime getStarted() {
        return started;
    }

    public LocalDateTime getCompleted() {
        return completed;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMsgType() {
        return msgType;
    }

    public FlowKey toKey() {
        return new FlowKey(sender, receiver, msgType);
    }
}
