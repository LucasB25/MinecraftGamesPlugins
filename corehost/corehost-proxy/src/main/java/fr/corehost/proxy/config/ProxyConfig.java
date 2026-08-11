package fr.corehost.proxy.config;

public class ProxyConfig {
    
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private String discordBotToken;
    private String discordBotId;
    private String prefix;
    private String lobbyKeyword;
    
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUser;
    private String dbPassword;

    public ProxyConfig() {
        this.redisHost = "127.0.0.1";
        this.redisPort = 6379;
        this.redisPassword = "";
        this.discordBotToken = "";
        this.discordBotId = "";
        this.prefix = "&8[&6CoreHost&8] &7";
        this.lobbyKeyword = "lobby";
        
        this.dbHost = "127.0.0.1";
        this.dbPort = 3306;
        this.dbDatabase = "corehost";
        this.dbUser = "root";
        this.dbPassword = "";
    }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }

    public String getDiscordBotToken() { return discordBotToken; }
    public void setDiscordBotToken(String discordBotToken) { this.discordBotToken = discordBotToken; }

    public String getDiscordBotId() { return discordBotId; }
    public void setDiscordBotId(String discordBotId) { this.discordBotId = discordBotId; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getLobbyKeyword() { return lobbyKeyword; }
    public void setLobbyKeyword(String lobbyKeyword) { this.lobbyKeyword = lobbyKeyword; }

    public String getDbHost() { return dbHost; }
    public void setDbHost(String dbHost) { this.dbHost = dbHost; }

    public int getDbPort() { return dbPort; }
    public void setDbPort(int dbPort) { this.dbPort = dbPort; }

    public String getDbDatabase() { return dbDatabase; }
    public void setDbDatabase(String dbDatabase) { this.dbDatabase = dbDatabase; }

    public String getDbUser() { return dbUser; }
    public void setDbUser(String dbUser) { this.dbUser = dbUser; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    private int partyLimitDefault = 4;
    private int friendLimitDefault = 50;

    public int getPartyLimitDefault() { return partyLimitDefault; }
    public void setPartyLimitDefault(int partyLimitDefault) { this.partyLimitDefault = partyLimitDefault; }

    public int getFriendLimitDefault() { return friendLimitDefault; }
    public void setFriendLimitDefault(int friendLimitDefault) { this.friendLimitDefault = friendLimitDefault; }
}
