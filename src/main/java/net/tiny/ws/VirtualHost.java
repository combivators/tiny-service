package net.tiny.ws;

public class VirtualHost {

    private String domain;
    private String home;
    private String log;

    public String domain() {
        return domain;
    }
    public VirtualHost domain(String domain) {
        this.domain = domain;
        return this;
    }
    public String home() {
        return home;
    }
    public VirtualHost home(String home) {
        this.home = home;
        return this;
    }
    public String log() {
        return log;
    }
    public VirtualHost log(String log) {
        this.log = log;
        return this;
    }
}
