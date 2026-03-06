package com.lime.authguard.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public final class AuthGuard {

    private static final Gson GSON = new Gson();

    private static final String[] BANNER = {
            "",
            "§b █████╗ ██╗   ██╗████████╗██╗  ██╗ §3 ██████╗ ██╗   ██╗ █████╗ ██████╗ ██████╗ ",
            "§b██╔══██╗██║   ██║╚══██╔══╝██║  ██║ §3██╔════╝ ██║   ██║██╔══██╗██╔══██╗██╔══██╗",
            "§b███████║██║   ██║   ██║   ███████║ §3██║  ███╗██║   ██║███████║██████╔╝██║  ██║",
            "§b██╔══██║██║   ██║   ██║   ██╔══██║ §3██║   ██║██║   ██║██╔══██║██╔══██╗██║  ██║",
            "§b██║  ██║╚██████╔╝   ██║   ██║  ██║ §3╚██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝",
            "§b╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝ §3 ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ",
            ""
    };

    private AuthGuard() {
    }

    public static VerificationResult verify(JavaPlugin plugin, String licenseKey, String productId, String productName,
            String authServerUrl) {
        Logger logger = plugin.getLogger();

        for (String line : BANNER) {
            Bukkit.getConsoleSender().sendMessage(line);
        }

        Bukkit.getConsoleSender()
                .sendMessage("§8§m                                                                          ");
        Bukkit.getConsoleSender().sendMessage("§b  Product: §f" + productName + " §8| §bLicense Verification");
        Bukkit.getConsoleSender()
                .sendMessage("§8§m                                                                          ");

        ServerInfo serverInfo = ServerInfo.collect(plugin);

        Bukkit.getConsoleSender().sendMessage("§7  Server: §f" + serverInfo.getServerVersion());
        Bukkit.getConsoleSender()
                .sendMessage("§7  Address: §f" + serverInfo.getServerIp() + ":" + serverInfo.getServerPort());
        Bukkit.getConsoleSender().sendMessage("§7  Plugin Version: §f" + serverInfo.getPluginVersion());
        Bukkit.getConsoleSender().sendMessage(
                "§7  Java: §f" + serverInfo.getJavaVersion() + " §8| §7OS: §f" + serverInfo.getOperatingSystem());
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§e  ⏳ Verifying license...");

        try {
            VerificationResult result = sendVerificationRequest(authServerUrl, licenseKey, productId, serverInfo);

            if (result.isValid()) {
                Bukkit.getConsoleSender().sendMessage("");
                Bukkit.getConsoleSender().sendMessage("§a  ✅ License verified successfully!");
                Bukkit.getConsoleSender().sendMessage("§a  Product: §f" + productName);
                if (result.getDiscordUsername() != null) {
                    Bukkit.getConsoleSender().sendMessage("§a  Licensed to: §f" + result.getDiscordUsername());
                }
                if (result.getExpiresAt() != null) {
                    Bukkit.getConsoleSender().sendMessage("§a  Expires: §f" + result.getExpiresAt());
                }
                if (result.getIpUsage() != null) {
                    Bukkit.getConsoleSender().sendMessage("§a  IP Usage: §f" + result.getIpUsage());
                }
                if (result.getHwidUsage() != null) {
                    Bukkit.getConsoleSender().sendMessage("§a  HWID Usage: §f" + result.getHwidUsage());
                }
                Bukkit.getConsoleSender()
                        .sendMessage("§8§m                                                                          ");
                Bukkit.getConsoleSender().sendMessage("");
                return result;
            } else {
                printFailure(plugin, productName, result.getMessage());
                return result;
            }
        } catch (Exception e) {
            logger.severe("License verification error: " + e.getMessage());
            VerificationResult failResult = new VerificationResult(false, "Connection error: " + e.getMessage());
            printFailure(plugin, productName, failResult.getMessage());
            return failResult;
        }
    }

    public static VerificationResult verifyAndShutdown(JavaPlugin plugin, String licenseKey, String productId,
            String productName, String authServerUrl) {
        VerificationResult result = verify(plugin, licenseKey, productId, productName, authServerUrl);
        if (!result.isValid()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().disablePlugin(plugin);
            });
        }
        return result;
    }

    private static void printFailure(JavaPlugin plugin, String productName, String reason) {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§c  ❌ License verification failed!");
        Bukkit.getConsoleSender().sendMessage("§c  Product: §f" + productName);
        Bukkit.getConsoleSender().sendMessage("§c  Reason: §f" + reason);
        Bukkit.getConsoleSender()
                .sendMessage("§8§m                                                                          ");
        Bukkit.getConsoleSender().sendMessage("§c  Please check your license key or contact support.");
        Bukkit.getConsoleSender()
                .sendMessage("§8§m                                                                          ");
        Bukkit.getConsoleSender().sendMessage("");
    }

    private static VerificationResult sendVerificationRequest(String authServerUrl, String licenseKey, String productId,
            ServerInfo serverInfo) {
        HttpURLConnection connection = null;
        try {
            String baseUrl = authServerUrl.endsWith("/") ? authServerUrl.substring(0, authServerUrl.length() - 1)
                    : authServerUrl;
            String queryString = serverInfo.toQueryString(licenseKey, productId);
            URL url = new URL(baseUrl + "/api/v1/verify?" + queryString);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AuthGuard-SDK/1.0.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            String responseBody;

            if (responseCode >= 200 && responseCode < 300) {
                responseBody = readStream(connection);
            } else {
                responseBody = readErrorStream(connection);
            }

            return parseResponse(responseBody);
        } catch (Exception e) {
            throw new AuthGuardException("Failed to connect to auth server: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readStream(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String readErrorStream(HttpURLConnection connection) {
        try {
            if (connection.getErrorStream() != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "{\"valid\":false,\"message\":\"HTTP " + getResponseCodeSafe(connection) + "\"}";
    }

    private static int getResponseCodeSafe(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (Exception e) {
            return 0;
        }
    }

    private static VerificationResult parseResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);

            boolean valid = json.has("valid") && json.get("valid").getAsBoolean();
            String message = json.has("message") ? json.get("message").getAsString() : "Unknown";

            if (json.has("license")) {
                JsonObject license = json.getAsJsonObject("license");
                String productId = getJsonString(license, "productId");
                String expiresAt = getJsonString(license, "expiresAt");
                if (expiresAt == null)
                    expiresAt = "Lifetime";
                String discordUsername = getJsonString(license, "discordUsername");

                String ipUsage = getJsonString(license, "ipUsage");
                if (ipUsage == null && license.has("activeIps") && license.has("ipLimit")) {
                    int ipCount = 0;
                    if (license.get("activeIps").isJsonArray()) {
                        ipCount = license.getAsJsonArray("activeIps").size();
                    }
                    int ipLimit = license.get("ipLimit").getAsInt();
                    ipUsage = ipCount + "/" + ipLimit;
                }

                String hwidUsage = getJsonString(license, "hwidUsage");

                return new VerificationResult(valid, message, productId, expiresAt, ipUsage, hwidUsage,
                        discordUsername);
            }

            String productId = getJsonString(json, "product_id");
            String expiresAt = getJsonString(json, "expires_at");
            String ipUsage = getJsonString(json, "ip_usage");
            String hwidUsage = getJsonString(json, "hwid_usage");

            return new VerificationResult(valid, message, productId, expiresAt, ipUsage, hwidUsage, null);
        } catch (Exception e) {
            return new VerificationResult(false, "Failed to parse server response");
        }
    }

    private static String getJsonString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
