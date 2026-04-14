package se.forsman.bevakning.domain;

import java.util.Objects;

public class FlowKey {
    private final String sender;
    private final String receiver;
    private final String msgType;

    public FlowKey(String sender, String receiver, String msgType) {
        this.sender = sender;
        this.receiver = receiver;
        this.msgType = msgType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowKey)) return false;
        FlowKey flowKey = (FlowKey) o;
        return Objects.equals(sender, flowKey.sender)
                && Objects.equals(receiver, flowKey.receiver)
                && Objects.equals(msgType, flowKey.msgType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, receiver, msgType);
    }
}
