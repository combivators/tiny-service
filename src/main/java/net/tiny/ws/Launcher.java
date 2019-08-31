package net.tiny.ws;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());

    private EmbeddedServer.Builder builder;
    private EmbeddedServer server;

    public EmbeddedServer.Builder getBuilder() {
        if (builder == null) {
        	builder = new EmbeddedServer.Builder();
        }
        return builder;
    }

    @Override
    public void run() {
    	if (isStarting()) {
        	LOGGER.warning(String.format("[BOOT] Server launcher already started!", builder.toString()));
        	return;
    	}

        server = builder.build();
        server.listen(callback -> {
            if(callback.success()) {
                LOGGER.info(String.format("[BOOT] Server'%s' launcher successful start.", builder.toString()));
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                }
                LOGGER.info(String.format("[BOOT] Server'%s' launcher stopped.", builder.toString()));
            } else {
            	Throwable err = callback.cause();
            	LOGGER.log(Level.SEVERE,
            			String.format("[BOOT] Server'%s' launcher startup failed - '%s'", builder.toString(), err.getMessage()), err);
            }
        });
    }

    public boolean isStarting() {
        return server != null && server.isStarted();
    }

    public void stop() {
        if (isStarting()) {
        	if (server.isStarted()) {
	            server.stop();
	            server = null;
        	}
        }
    }
}