package net.tiny.ws;

public interface Controllable {

    boolean isStarted();
    boolean start();
    void stop();
    void suspend();
    void resume();
    String status();

    boolean hasError();
    Throwable getLastError();

    Object getAttribute(String name);
    void setAttribute(String name, Object value);
}
