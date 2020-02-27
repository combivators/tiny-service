package net.tiny.ws;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.tiny.service.Statable;
import net.tiny.service.Trigger;

public class Launcher extends Trigger implements Runnable, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

    private EmbeddedServer.Builder builder;
    private EmbeddedServer server;

    public EmbeddedServer.Builder getBuilder() {
        if (builder == null) {
            builder = new EmbeddedServer.Builder();
        }
        states(Statable.States.NONE);
        return builder;
    }

    @Override
    public void run() {
        if (isStarting()) {
            LOGGER.warning(String.format("[BOOT] Server launcher already started!", builder.toString()));
            return;
        }

        states(Statable.States.READY);
        try {
            await();
            startup();
        } catch (InterruptedException e) {
            states(Statable.States.FAILED);
        }
    }

    void startup() {
        server = builder.build();
        server.listen(callback -> {
            if(callback.success()) {
                LOGGER.info(String.format("[BOOT] Server'%s' launcher successful start.", builder.toString()));
                states(Statable.States.ALIVE);
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                }
                LOGGER.info(String.format("[BOOT] Server'%s' launcher stopped.", builder.toString()));
            } else {
                Throwable err = callback.cause();
                LOGGER.log(Level.SEVERE,
                        String.format("[BOOT] Server'%s' launcher startup failed - '%s'", builder.toString(), err.getMessage()), err);
                states(Statable.States.FAILED);
            }
        });
    }
    public boolean isStarting() {
        return server != null && server.isStarted();
    }

    public void stop() {
        if (isStarting()) {
            server.stop();
            states(Statable.States.DONE);
            server = null;
        }
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    @Override
    public String toString() {
        return getBuilder().toString();
    }
}