package com.lime.authguard.sdk;

public class VerificationResult {

    private final boolean valid;
    private final String message;
    private final String productId;
    private final String expiresAt;
    private final String ipUsage;
    private final String hwidUsage;
    private final String discordUsername;
    private final boolean signatureVerified;

    public VerificationResult(boolean valid, String message, String productId, String expiresAt, String ipUsage,
            String hwidUsage, String discordUsername) {
        this(valid, message, productId, expiresAt, ipUsage, hwidUsage, discordUsername, false);
    }

    public VerificationResult(boolean valid, String message, String productId, String expiresAt, String ipUsage,
            String hwidUsage, String discordUsername, boolean signatureVerified) {
        this.valid = valid;
        this.message = message;
        this.productId = productId;
        this.expiresAt = expiresAt;
        this.ipUsage = ipUsage;
        this.hwidUsage = hwidUsage;
        this.discordUsername = discordUsername;
        this.signatureVerified = signatureVerified;
    }

    public VerificationResult(boolean valid, String message) {
        this(valid, message, null, null, null, null, null, false);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public String getProductId() {
        return productId;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public String getIpUsage() {
        return ipUsage;
    }

    public String getHwidUsage() {
        return hwidUsage;
    }

    public String getDiscordUsername() {
        return discordUsername;
    }

    /**
     * Returns {@code true} if the server's signed nonce response was
     * cryptographically verified. Returns {@code false} if the server
     * does not support response signing or if verification was skipped.
     *
     * @return whether the response signature was verified
     */
    public boolean isSignatureVerified() {
        return signatureVerified;
    }
}
