package com.lime.authguard.sdk;

/**
 * Internal result of the signed nonce verification step.
 * Used by {@link SignatureVerifier} to communicate outcome to {@link AuthGuard}.
 */
class SignatureResult {

    private final boolean verified;
    private final boolean serverSupportsSignature;
    private final String failureReason;

    private SignatureResult(boolean verified, boolean serverSupportsSignature, String failureReason) {
        this.verified = verified;
        this.serverSupportsSignature = serverSupportsSignature;
        this.failureReason = failureReason;
    }

    static SignatureResult success() {
        return new SignatureResult(true, true, null);
    }

    static SignatureResult unsupported() {
        return new SignatureResult(false, false, "Server does not support response signing");
    }

    static SignatureResult failure(String reason) {
        return new SignatureResult(false, true, reason);
    }

    boolean isVerified() {
        return verified;
    }

    boolean isServerSupportsSignature() {
        return serverSupportsSignature;
    }

    String getFailureReason() {
        return failureReason;
    }
}
