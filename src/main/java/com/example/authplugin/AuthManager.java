package com.example.authplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuthManager {
    private final Set<UUID> authenticatedPlayers = new HashSet<>();
    private final Map<UUID, String> playerPasswords = new HashMap<>();
    private final AuthPlugin plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final File passwordFile;
    private final File configFile;
    private List<String> allowedOfflinePlayers;
    private String denyMessage;
    private final Map<UUID, Integer> loginAttempts = new HashMap<>();
    private final Map<UUID, Long> lastLoginAttempt = new HashMap<>();
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final long LOGIN_TIMEOUT = 300000; // 5分钟

    @Inject
    public AuthManager(AuthPlugin plugin, ProxyServer server, Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        
        this.passwordFile = new File("plugins/auth-plugin/passwords.txt");
        this.configFile = new File("plugins/auth-plugin/config.yml");
        this.allowedOfflinePlayers = new ArrayList<>();
        this.denyMessage = "§c对不起，该用户不允许离线登录！请联系管理员";
        loadConfig();
        loadPasswords();
    }

    private void loadConfig() {
        try {
            // 确保目录存在
            configFile.getParentFile().mkdirs();
            
            // 如果配置文件不存在，从资源中复制
            if (!configFile.exists()) {
                try (InputStream in = plugin.getClass().getResourceAsStream("/config.yml")) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 加载配置
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .file(configFile)
                .build();
            
            ConfigurationNode root = loader.load();
            
            List<String> players = new ArrayList<>();
            ConfigurationNode playersNode = root.node("allowed-offline-players");
            if (!playersNode.virtual()) {
                for (ConfigurationNode node : playersNode.childrenList()) {
                    String player = node.getString();
                    if (player != null) {
                        players.add(player);
                    }
                }
            }
            allowedOfflinePlayers = players;
            
            denyMessage = root.node("deny-message").getString(denyMessage);
        } catch (Exception e) {
            logger.error("无法加载配置文件", e);
        }
    }

    public boolean canPlayerJoin(Player player) {
        // 获取 Velocity 的 online-mode 设置
        boolean velocityOnlineMode = this.server.getConfiguration().isOnlineMode();
        
        // 如果 Velocity 设置为 online-mode=true
        if (velocityOnlineMode) {
            // 只允许正版玩家
            if (!player.isOnlineMode()) {
                logger.info("玩家 " + player.getUsername() + " 尝试使用离线账户连接，但服务器要求正版验证");
                return false;
            }
            logger.info("正版玩家 " + player.getUsername() + " 正在连接...");
            return true;
        } else {
            // 如果 Velocity 设置为 online-mode=false
            // 正版玩家直接允许
            if (player.isOnlineMode()) {
                logger.info("正版玩家 " + player.getUsername() + " 正在连接...");
                return true;
            }
            
            // 检查离线玩家是否在白名单中
            boolean allowed = allowedOfflinePlayers.contains(player.getUsername());
            if (!allowed) {
                logger.info("离线玩家 " + player.getUsername() + " 尝试连接但不在白名单中");
            } else {
                logger.info("白名单离线玩家 " + player.getUsername() + " 正在连接...");
            }
            return allowed;
        }
    }

    public String getDenyMessage() {
        boolean velocityOnlineMode = this.server.getConfiguration().isOnlineMode();
        if (velocityOnlineMode) {
            return "§c服务器已开启正版验证，请使用正版账户进入！";
        }
        return denyMessage;
    }

    private void loadPasswords() {
        try {
            // 确保目录存在
            passwordFile.getParentFile().mkdirs();
            
            // 如果文件不存在，创建它
            if (!passwordFile.exists()) {
                passwordFile.createNewFile();
                return;
            }

            // 读取密码文件
            try (BufferedReader reader = new BufferedReader(new FileReader(passwordFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 3) {
                        UUID uuid = UUID.fromString(parts[0]);
                        playerPasswords.put(uuid, parts[2]);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("无法加载密码文件", e);
        }
    }

    private void savePasswords() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(passwordFile))) {
            for (Map.Entry<UUID, String> entry : playerPasswords.entrySet()) {
                Player player = plugin.getServer().getPlayer(entry.getKey()).orElse(null);
                if (player != null) {
                    // 格式：UUID:玩家名:密码
                    writer.println(entry.getKey() + ":" + player.getUsername() + ":" + entry.getValue());
                }
            }
        } catch (IOException e) {
            logger.error("无法保存密码文件", e);
        }
    }

    public boolean isAuthenticated(Player player) {
        // 正版玩家直接返回 true
        if (player.isOnlineMode()) {
            return true;
        }
        // 非正版玩家检查是否已登录
        return authenticatedPlayers.contains(player.getUniqueId());
    }

    public boolean isRegistered(Player player) {
        return playerPasswords.containsKey(player.getUniqueId());
    }

    public boolean register(Player player, String password) {
        if (isRegistered(player)) {
            player.sendMessage(Component.text("§c你已经注册过了！"));
            return false;
        }

        if (!isPasswordStrong(password)) {
            player.sendMessage(Component.text("§c密码必须至少包含6个字符，包括大小写字母和数字！"));
            return false;
        }
        
        playerPasswords.put(player.getUniqueId(), password);
        authenticatedPlayers.add(player.getUniqueId());
        savePasswords();  // 保存到文件
        
        player.sendMessage(Component.text("§a注册成功！"));
        
        // 获取生存服务器
        Optional<RegisteredServer> survivalServer = plugin.getServer().getServer("survival");
        if (survivalServer.isPresent()) {
            // 创建连接请求并送
            player.createConnectionRequest(survivalServer.get()).fireAndForget();
            player.sendMessage(Component.text("§a正在将你传送到生存服务器..."));
        } else {
            player.sendMessage(Component.text("§c错误：找不到生存服务器，请联系管理员！"));
        }
        
        return true;
    }

    public boolean authenticate(Player player, String password) {
        UUID playerId = player.getUniqueId();
        
        // 检查是否在超时时间内
        Long lastAttempt = lastLoginAttempt.get(playerId);
        if (lastAttempt != null && System.currentTimeMillis() - lastAttempt < LOGIN_TIMEOUT) {
            int attempts = loginAttempts.getOrDefault(playerId, 0);
            if (attempts >= MAX_LOGIN_ATTEMPTS) {
                player.sendMessage(Component.text("§c登录尝试次数过多，请等待5分钟后再试"));
                return false;
            }
        } else {
            // 重置尝试次数
            loginAttempts.remove(playerId);
        }

        if (!isRegistered(player)) {
            player.sendMessage(Component.text("§c你还没有注册！请使用 /register <密码> 注册"));
            return false;
        }

        // 更新最后尝试时间
        lastLoginAttempt.put(playerId, System.currentTimeMillis());

        if (password.equals(playerPasswords.get(playerId))) {
            authenticatedPlayers.add(playerId);
            player.sendMessage(Component.text("§a登录成功！"));
            
            // 获取生存服务器
            Optional<RegisteredServer> survivalServer = plugin.getServer().getServer("survival");
            if (survivalServer.isPresent()) {
                // 创建连接请求发送
                player.createConnectionRequest(survivalServer.get()).fireAndForget();
                player.sendMessage(Component.text("§a正在将你传送到生存服务器..."));
            } else {
                player.sendMessage(Component.text("§c错误：找不到生存服务器，请联系管理员！"));
            }
            
            return true;
        }
        
        // 增加失败次数
        loginAttempts.put(playerId, loginAttempts.getOrDefault(playerId, 0) + 1);
        player.sendMessage(Component.text("§c密码错误！"));
        return false;
    }

    public void removePlayer(Player player) {
        authenticatedPlayers.remove(player.getUniqueId());
    }

    public void reloadConfig() {
        loadConfig();
        logger.info("配置已重载");
    }

    public boolean shouldAuthenticate(Player player) {
        // 如果是正版玩家，则不需要验证
        if (player.isOnlineMode()) {
            return false;
        }
        // 非正版玩家需要验证
        return !authenticatedPlayers.contains(player.getUniqueId());
    }

    private boolean checkPremiumAccount(String username) {
        try {
            // 构建 Mojang API URL
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // 状态码 200 表示找到了这个正版账户
                return true;
            } else if (responseCode == 404) {
                // 状态码 404 表示这不是正版账户
                return false;
            }
        } catch (Exception e) {
            logger.error("检查正版账户时发生错误", e);
        }
        return false;
    }

    private boolean isPremiumPlayer(Player player) {
        try {
            // 构建 Mojang API URL
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + player.getUsername());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // 如果找到了玩家，说明这个用户名是正版账户
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.readLine();
                
                // 如果找到了响应，说明这是一个正版用户名
                if (response != null && !response.isEmpty()) {
                    // 这里我们可以选择是否要验证 UUID
                    // 由于我们是在 offline mode 下运行，UUID 会不同，所以这里只检查用户名
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("检查正版账户时发生错误", e);
        }
        return false;
    }

    private boolean verifyPremiumSession(Player player, String username) {
        try {
            // 首先检查用户名是否是正版账户
            URL checkUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection checkConn = (HttpURLConnection) checkUrl.openConnection();
            checkConn.setRequestMethod("GET");
            checkConn.setConnectTimeout(5000);
            checkConn.setReadTimeout(5000);

            if (checkConn.getResponseCode() != 200) {
                return false;
            }

            // 然后验证会话
            URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + username);
            HttpURLConnection sessionConn = (HttpURLConnection) sessionUrl.openConnection();
            sessionConn.setRequestMethod("GET");
            sessionConn.setConnectTimeout(5000);
            sessionConn.setReadTimeout(5000);

            if (sessionConn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(sessionConn.getInputStream()));
                String response = reader.readLine();
                
                // 检查响应中是否包含玩家信息
                if (response != null && !response.isEmpty()) {
                    // 可以进一步解析响应获取更多信息（如皮肤等）
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("验证正版会话时发生错误", e);
        }
        return false;
    }

    public void handlePlayerJoin(Player player) {
        String username = player.getUsername();
        
        // 异步验证会话
        server.getScheduler().buildTask(plugin, () -> {
            if (isPremiumPlayer(username)) {
                // 如果是正版用户名，要求玩家证明身份
                player.sendMessage(Component.text("§e检测到此用户名为正版账号"));
                player.sendMessage(Component.text("§e如果这是你的账号，请使用正版客户端登录"));
                player.sendMessage(Component.text("§e否则请更换用户名后使用离线登录"));
                
                // 要求玩家登录
                if (!isRegistered(player)) {
                    player.sendMessage(Component.text("§e请使用 /register <密码> 注册"));
                } else {
                    player.sendMessage(Component.text("§e请使用 /login <密码> 登录"));
                }
            } else {
                // 非正版用户名，走普通注册/登录流程
                if (!isRegistered(player)) {
                    player.sendMessage(Component.text("§e请使用 /register <密码> 注册"));
                } else {
                    player.sendMessage(Component.text("§e请使用 /login <密码> 登录"));
                }
            }
        }).schedule();
    }

    public void authenticatePlayer(UUID uuid) {
        authenticatedPlayers.add(uuid);
    }

    private boolean isPremiumPlayer(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            logger.error("检查正版账户时发生错误", e);
            return false;
        }
    }

    private boolean isPasswordStrong(String password) {
        // 密码长度至少6位
        if (password.length() < 6) {
            return false;
        }
        
        // 检查是否包含大写字母
        boolean hasUpperCase = false;
        // 检查是否包含小写字母
        boolean hasLowerCase = false;
        // 检查是否包含数字
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        
        // 必须同时包含大写字母、小写字母和数字
        return hasUpperCase && hasLowerCase && hasDigit;
    }
} 