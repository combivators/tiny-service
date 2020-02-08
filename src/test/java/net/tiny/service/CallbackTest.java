package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;


public class CallbackTest {

    @Test
    public void testSyncCallback() {
        callSync(callback -> {
            if(callback.success()) {
                System.out.println("Sync call successed!");
            } else {
                System.out.println("Sync call failed.");
            }
        });
    }

    @Test
    public void testAsyncCallback() {
        callAsync(callback -> {
            if(callback.success()) {
                System.out.println("Async call successed!");
            } else {
                System.out.println("Async call failed.");
            }
        });
    }

    private <T> void callSync(Consumer<Callback<T>> consumer) {
        System.out.println("Sync searching...");
        Callback<T> callback;
        try {
            T t = null;
            callback = Callback.succeed(t);
        } catch (Exception e) {
            callback = Callback.failed(e);
        }
        consumer.accept(callback);
    }

    private <T> void callAsync(Consumer<Callback<T>> consumer) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Callback<T> callback;
        Future<T> future
            = executor.submit(new Callable<T>() {
                @Override
                public T call() {
                    //searcher.search(target);
                    System.out.println("Async searching...");
                    T t = null;
                    return t;
                }});
        try {
            callback = Callback.succeed(future.get());
        } catch (InterruptedException | ExecutionException e) {
            callback = Callback.failed(e);
        }
        consumer.accept(callback);
    }


    @Test
    public void testStateAfterCompletion() {
        String foo = new String("the-value");
        Callback<Object> callback = Callback.succeed(foo);
        assertTrue(callback.success());
        assertFalse(callback.fail());
        assertTrue(callback.isComplete());
        assertEquals(foo, callback.result());
        assertNull(callback.cause());
        Exception cause = new Exception();

        callback = Callback.failed(cause);
        assertFalse(callback.success());
        assertTrue(callback.fail());
        assertTrue(callback.isComplete());
        assertNull(callback.result());
        assertEquals(cause, callback.cause());
    }


}
