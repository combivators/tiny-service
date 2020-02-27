package net.tiny.service;

import java.util.function.Consumer;

public interface Statable {
    enum States {
        NONE,
        READY,
        ALIVE,
        FAILED,
        DONE,
    }

    States states();

    void trigger(Consumer<States> consumer);
}
