package se.forsman.bevakning.domain;

import java.time.LocalDate;

public class DailyFlowCount {
    private final LocalDate flowDate;
    private final String sender;
    private final String receiver;
    private final String msgType;
    private final int count;

    public DailyFlowCount(LocalDate flowDate, String sender, String receiver, String msgType, int count) {
        this.flowDate = flowDate;
        this.sender = sender;
        this.receiver = receiver;
        this.msgType = msgType;
        this.count = count;
    }

    public LocalDate getFlowDate() {
        return flowDate;
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

    public int getCount() {
        return count;
    }

    public FlowKey toKey() {
        return new FlowKey(sender, receiver, msgType);
    }
}
