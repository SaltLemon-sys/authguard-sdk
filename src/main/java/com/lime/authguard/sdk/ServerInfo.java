package com.lime.authguard.sdk;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public class ServerInfo {

    private final String serverIp;
    private final int serverPort;
    private final String serverVersion;
    private final String pluginVersion;
    private final String hwid;
    private final String macAddress;
    private final String operatingSystem;
    private final String osVersion;
    private final String osArch;
    private final String javaVersion;

    private ServerInfo(String serverIp, int serverPort, String serverVersion, String pluginVersion,
            String hwid, String macAddress, String operatingSystem, String osVersion,
            String osArch, String javaVersion) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.serverVersion = serverVersion;
        this.pluginVersion = pluginVersion;
        this.hwid = hwid;
        this.macAddress = macAddress;
        this.operatingSystem = operatingSystem;
        this.osVersion = osVersion;
        this.osArch = osArch;
        this.javaVersion = javaVersion;
    }

    public static ServerInfo collect(JavaPlugin plugin) {
        String serverIp = resolveServerIp();
        int serverPort = Bukkit.getPort();
        String serverVersion = Bukkit.getVersion();
        String pluginVersion = plugin.getDescription().getVersion();
        String macAddress = resolveMacAddress();
        String hwid = loadOrGenerateHwid(plugin);
        String operatingSystem = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String javaVersion = System.getProperty("java.version");

        return new ServerInfo(serverIp, serverPort, serverVersion, pluginVersion,
                hwid, macAddress, operatingSystem, osVersion, osArch, javaVersion);
    }

    public String toQueryString(String licenseKey, String productId) {
        StringBuilder sb = new StringBuilder();
        sb.append("key=").append(encode(licenseKey));
        sb.append("&product=").append(encode(productId));
        sb.append("&ip=").append(encode(serverIp + ":" + serverPort));
        sb.append("&version=").append(encode(pluginVersion));
        sb.append("&hwid=").append(encode(hwid));
        sb.append("&mac=").append(encode(macAddress));
        sb.append("&os=").append(encode(operatingSystem));
        sb.append("&osVersion=").append(encode(osVersion));
        sb.append("&osArch=").append(encode(osArch));
        sb.append("&javaVersion=").append(encode(javaVersion));
        sb.append("&serverVersion=").append(encode(serverVersion));
        sb.append("&serverPort=").append(serverPort);
        return sb.toString();
    }

    private static String encode(String value) {
        if (value == null)
            return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String resolveServerIp() {
        try {
            String configIp = Bukkit.getIp();
            if (configIp != null && !configIp.isEmpty() && !configIp.equals("0.0.0.0")) {
                return configIp;
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String resolveMacAddress() {
        try {
            List<String> macs = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1)
                            sb.append(":");
                    }
                    macs.add(sb.toString());
                }
            }
            if (!macs.isEmpty()) {
                Collections.sort(macs);
                return macs.get(0);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static String resolveAllMacs() {
        try {
            List<String> macs = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1)
                            sb.append(":");
                    }
                    macs.add(sb.toString());
                }
            }
            if (!macs.isEmpty()) {
                Collections.sort(macs);
                return String.join("+", macs);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private static String loadOrGenerateHwid(JavaPlugin plugin) {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File hwidFile = new File(dataFolder, ".hwid");
            if (hwidFile.exists()) {
                String saved = Files.readString(hwidFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!saved.isEmpty() && !saved.equals("unknown")) {
                    return saved;
                }
            }
            String hwid = generateHwid();
            Files.writeString(hwidFile.toPath(), hwid, StandardCharsets.UTF_8);
            return hwid;
        } catch (Exception e) {
            return generateHwid();
        }
    }

    private static String generateHwid() {
        try {
            String seed = UUID.randomUUID().toString() + "|" +
                    System.getProperty("os.name") + "|" +
                    System.getProperty("os.arch") + "|" +
                    System.getProperty("user.name") + "|" +
                    Runtime.getRuntime().availableProcessors() + "|" +
                    System.nanoTime();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getHwid() {
        return hwid;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getJavaVersion() {
        return javaVersion;
    }
}
