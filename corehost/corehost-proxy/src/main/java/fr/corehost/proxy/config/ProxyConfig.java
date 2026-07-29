package fr.corehost.proxy.config;

public class ProxyConfig {
    
    private String redisHost;
    private int redisPort;
    private String redisPassword;

    public ProxyConfig() {
        this.redisHost = "127.0.0.1";
        this.redisPort = 6379;
        this.redisPassword = "";
    }

    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }

    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }

    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }
}
