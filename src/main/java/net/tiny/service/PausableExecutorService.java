package net.tiny.service;

import java.util.concurrent.ExecutorService;

public interface PausableExecutorService extends ExecutorService {
	void pause();
	void resume();
}
