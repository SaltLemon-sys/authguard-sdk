# AuthGuard SDK

Lightweight license verification library for Spigot/Paper plugins.

## Installation

### Maven (JitPack)

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
        <version>v1.0.0</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

## Usage

### Basic (Verify + Auto Disable on Failure)

```java
import com.lime.authguard.sdk.AuthGuard;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        String licenseKey = getConfig().getString("license-key");
        AuthGuard.verifyAndShutdown(this, licenseKey, "your-product-id", "MyPlugin", "https://your-authguard-server.com");
    }
}
```

### Manual Handling

```java
import com.lime.authguard.sdk.AuthGuard;
import com.lime.authguard.sdk.VerificationResult;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        String licenseKey = getConfig().getString("license-key");
        VerificationResult result = AuthGuard.verify(this, licenseKey, "your-product-id", "MyPlugin", "https://your-authguard-server.com");
        
        if (!result.isValid()) {
            getLogger().severe("License invalid: " + result.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        getLogger().info("License expires: " + result.getExpiresAt());
    }
}
```

### Plugin config.yml

```yaml
license-key: "XXXX-XXXX-XXXX-XXXX"
```

## What Gets Collected

The SDK automatically collects and sends the following info to your AuthGuard server:

| Field | Source |
|-------|--------|
| Server IP | `Bukkit.getIp()` / System detection |
| Server Port | `Bukkit.getPort()` |
| Server Version | `Bukkit.getVersion()` |
| Plugin Version | `plugin.yml` description |
| HWID | SHA-256 hash of MAC + OS + CPU |
| MAC Address | Primary network interface |
| OS | `os.name`, `os.version`, `os.arch` |
| Java Version | `java.version` |

## API Reference

### `AuthGuard.verify(plugin, licenseKey, productId, productName, authServerUrl)`
Verifies the license and prints the result to console. Returns `VerificationResult`.

### `AuthGuard.verifyAndShutdown(plugin, licenseKey, productId, productName, authServerUrl)`
Same as `verify()`, but automatically disables the plugin if verification fails.

### `VerificationResult`
| Method | Returns |
|--------|---------|
| `isValid()` | `boolean` |
| `getMessage()` | `String` |
| `getProductId()` | `String` |
| `getExpiresAt()` | `String` |
| `getIpUsage()` | `String` (e.g. "2/5") |
| `getHwidUsage()` | `String` (e.g. "1/1") |

## Build

```bash
cd authguard-sdk
mvn clean package
```
