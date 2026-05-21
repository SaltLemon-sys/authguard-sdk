# AuthGuard SDK

Lightweight license verification for Spigot/Paper plugins.

## Installation

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.SaltLemon-sys</groupId>
        <artifactId>authguard-sdk</artifactId>
        <version>1.0.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

## Usage

```java
@Override
public void onEnable() {
    saveDefaultConfig();
    String key = getConfig().getString("license-key");
    AuthGuard.verifyAndShutdown(this, key, "PRODUCT_ID", "PluginName", "http://your-server:port");
}
```

Plugin auto-disables if verification fails. License key goes in `config.yml`:

```yaml
license-key: "XXXX-XXXX-XXXX-XXXX"
```

## What Gets Sent

Server IP, port, version, plugin version, HWID, MAC address, OS info, Java version.

## Built-in Signed Verification

The SDK **automatically** performs signed nonce challenge-response verification against the AuthGuard server. No extra code is needed — it happens inside `verify()` and `verifyAndShutdown()`.

This protects against:
- **Replay attacks** — each request includes a unique cryptographic nonce
- **Response tampering** — the server signs responses with an RSA key, and the SDK verifies the signature
- **Stale responses** — responses older than 120 seconds are rejected

If the server supports signing, `VerificationResult.isSignatureVerified()` returns `true`. If it doesn't (older server versions), verification falls back to basic license validation — fully backward compatible.

## Response

| Method | Returns |
|--------|---------|
| `isValid()` | `boolean` |
| `getMessage()` | `String` |
| `getExpiresAt()` | `String` |
| `getIpUsage()` | `String` (e.g. "1/5") |
| `getHwidUsage()` | `String` (e.g. "1/1") |
| `getDiscordUsername()` | `String` |
| `isSignatureVerified()` | `boolean` |
