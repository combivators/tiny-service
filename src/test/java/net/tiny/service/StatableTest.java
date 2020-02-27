package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class StatableTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @Test
    public void testTrigger() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        OneTask one = new OneTask();
        TwoTask two = new TwoTask();

        // Three task waiting one an two done.
        ThreeTask three = new ThreeTask();
        three.getRequires().add(one);
        three.getRequires().add(two);

        executor.execute(one);
        executor.execute(two);
        assertEquals(Statable.States.NONE, three.states());
        executor.execute(three);
        Thread.sleep(600L);

        assertEquals(Statable.States.DONE, one.states());
        assertEquals(Statable.States.DONE, two.states());
        assertEquals(Statable.States.DONE, three.states());
    }

    static class OneTask extends Trigger implements Runnable {

        @Override
        public void run() {
            states(Statable.States.READY);
            try {
                Thread.sleep(300L);
                states(Statable.States.ALIVE);
                System.out.println("[ONE] doing something ...");
            } catch (InterruptedException e) {
                states(Statable.States.FAILED);
            } finally {
                states(Statable.States.DONE);
            }
        }
    }

    static class TwoTask extends Trigger implements Runnable {
        @Override
        public void run() {
            states(Statable.States.READY);
            try {
                System.out.println("[TWO] doing something ...");
                Thread.sleep(200L);
                states(Statable.States.ALIVE);
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                states(Statable.States.FAILED);
            } finally {
                states(Statable.States.DONE);
            }

        }
    }

    static class ThreeTask extends Trigger implements Runnable {

        @Override
        public void run() {
            states(Statable.States.NONE);
            try {
                System.out.println("[THREE] waitting ...");
                await();
                states(Statable.States.READY);
                System.out.println("[THREE] doing something ...");
                Thread.sleep(100L);
                states(Statable.States.ALIVE);
             } catch (InterruptedException e) {
                states(Statable.States.FAILED);
            } finally {
                states(Statable.States.DONE);
            }
        }
    }
}
