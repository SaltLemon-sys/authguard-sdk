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

## Response

| Method | Returns |
|--------|---------|
| `isValid()` | `boolean` |
| `getMessage()` | `String` |
| `getExpiresAt()` | `String` |
| `getIpUsage()` | `String` (e.g. "1/5") |
| `getHwidUsage()` | `String` (e.g. "1/1") |
| `getDiscordUsername()` | `String` |
