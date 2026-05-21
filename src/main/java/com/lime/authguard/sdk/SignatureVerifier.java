package com.lime.authguard.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal helper that performs signed nonce challenge-response verification
 * against the AuthGuard server. This class is used internally by {@link AuthGuard}
 * and is not part of the public SDK API.
 *
 * <p>The verification flow:
 * <ol>
 *   <li>Check that the server response contains a {@code verification} block</li>
 *   <li>Validate the returned nonce matches the one we sent</li>
 *   <li>Validate the signed timestamp is within the allowed time window</li>
 *   <li>Fetch the server's RSA public key from {@code /api/v1/verify/public-key}</li>
 *   <li>Verify the RSA signature over the canonical payload</li>
 * </ol>
 */
class SignatureVerifier {

    private static final Logger LOGGER = Logger.getLogger(SignatureVerifier.class.getName());
    private static final long DEFAULT_WINDOW_SECONDS = 120;

    private SignatureVerifier() {
    }

    /**
     * Verifies the signed challenge in the server response.
     *
     * @param response      the full JSON response from {@code /api/v1/verify}
     * @param expectedNonce the nonce we sent in the request
     * @param serverUrl     base server URL (no trailing slash)
     * @param windowSeconds max allowed age of the signed timestamp (0 or negative = use default 120s)
     * @return a {@link SignatureResult} indicating success, failure, or server-unsupported
     */
    static SignatureResult verify(JsonObject response, String expectedNonce, String serverUrl, long windowSeconds) {
        if (windowSeconds <= 0) {
            windowSeconds = DEFAULT_WINDOW_SECONDS;
        }

        // Step 1: Check if server returned a verification block
        if (!response.has("verification") || response.get("verification").isJsonNull()) {
            LOGGER.fine("Server did not return a signed verification block — skipping signature check");
            return SignatureResult.unsupported();
        }

        try {
            JsonObject verification = response.getAsJsonObject("verification");

            // Step 2: Validate nonce
            JsonObject payload = verification.getAsJsonObject("payload");
            if (payload == null) {
                return SignatureResult.failure("Verification block has no payload");
            }

            String returnedNonce = getJsonString(payload, "nonce");
            if (returnedNonce == null || !returnedNonce.equals(expectedNonce)) {
                return SignatureResult.failure("Nonce mismatch: expected " + expectedNonce + " but got " + returnedNonce);
            }

            // Step 3: Validate timestamp freshness
            String timestampStr = getJsonString(payload, "timestamp");
            if (timestampStr == null) {
                return SignatureResult.failure("Verification payload has no timestamp");
            }

            Instant signedAt;
            try {
                signedAt = Instant.parse(timestampStr);
            } catch (Exception e) {
                return SignatureResult.failure("Invalid timestamp format: " + timestampStr);
            }

            long ageSeconds = Math.abs(Duration.between(signedAt, Instant.now()).getSeconds());
            if (ageSeconds > windowSeconds) {
                return SignatureResult.failure("Signed response is too old: " + ageSeconds + "s (max " + windowSeconds + "s)");
            }

            // Step 4: Fetch server's public key
            String algorithm = getJsonString(verification, "algorithm");
            if (algorithm == null) {
                algorithm = "SHA256withRSA";
            }

            String signedPayload = getJsonString(verification, "signedPayload");
            String signatureBase64 = getJsonString(verification, "signature");

            if (signedPayload == null || signatureBase64 == null) {
                return SignatureResult.failure("Verification block missing signedPayload or signature");
            }

            PublicKey publicKey = fetchPublicKey(serverUrl);
            if (publicKey == null) {
                return SignatureResult.failure("Failed to fetch server public key from " + serverUrl);
            }

            // Step 5: Verify RSA signature
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            verifier.update(signedPayload.getBytes(StandardCharsets.UTF_8));

            boolean signatureValid = verifier.verify(Base64.getDecoder().decode(signatureBase64));
            if (!signatureValid) {
                return SignatureResult.failure("RSA signature verification failed — response may be tampered");
            }

            return SignatureResult.success();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Signature verification error", e);
            return SignatureResult.failure("Signature verification error: " + e.getMessage());
        }
    }

    /**
     * Fetches the server's RSA public key from the {@code /api/v1/verify/public-key} endpoint.
     */
    private static PublicKey fetchPublicKey(String serverUrl) {
        HttpURLConnection connection = null;
        try {
            String url = serverUrl + "/api/v1/verify/public-key";
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AuthGuard-SDK/1.0.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOGGER.warning("Public key endpoint returned HTTP " + status);
                return null;
            }

            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String publicKeyPem = getJsonString(json, "publicKey");
            if (publicKeyPem == null) {
                LOGGER.warning("Public key endpoint did not return a 'publicKey' field");
                return null;
            }

            return parsePublicKey(publicKeyPem);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch public key", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Parses a PEM-encoded RSA public key into a {@link PublicKey} object.
     */
    private static PublicKey parsePublicKey(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static String getJsonString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
