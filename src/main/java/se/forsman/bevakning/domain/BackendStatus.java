package se.forsman.bevakning.domain;

public class BackendStatus {
    private final boolean ok;
    private final String message;

    public BackendStatus(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }
}
