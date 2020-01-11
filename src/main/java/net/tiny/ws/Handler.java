package net.tiny.ws;

@FunctionalInterface
public interface Handler<E> {
    void handle(E event);
}
