package app.sdrpole.core.directory;

public record RadioReferenceCredentials(String appKey, String username, String password) {
    public RadioReferenceCredentials {
        if (appKey == null || appKey.isBlank()) throw new IllegalArgumentException("SDR-Pole needs an approved RadioReference application key");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Enter your RadioReference username");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Enter your RadioReference password");
    }
}
