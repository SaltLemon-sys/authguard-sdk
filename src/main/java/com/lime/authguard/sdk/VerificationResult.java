package com.lime.authguard.sdk;

public class VerificationResult {

    private final boolean valid;
    private final String message;
    private final String productId;
    private final String expiresAt;
    private final String ipUsage;
    private final String hwidUsage;

    public VerificationResult(boolean valid, String message, String productId, String expiresAt, String ipUsage,
            String hwidUsage) {
        this.valid = valid;
        this.message = message;
        this.productId = productId;
        this.expiresAt = expiresAt;
        this.ipUsage = ipUsage;
        this.hwidUsage = hwidUsage;
    }

    public VerificationResult(boolean valid, String message) {
        this(valid, message, null, null, null, null);
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
}
