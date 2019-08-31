package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WeakRefTest {

	@Test
    public void testThreadWeak() throws InterruptedException {
        final ReferenceQueue<Thread> queue = new ReferenceQueue<Thread>();
        List<WeakReference<Thread>> threads = new ArrayList<WeakReference<Thread>>(100);

        for (int i = 0; i < 100; i++) {
            Thread t = new Thread(new Runnable() {
                public void run() {

                }
            });
            threads.add(new WeakReference<Thread>(t, queue));
        }

        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(100);
        }

        Reference<? extends Thread> t = null;
        while ((t = queue.poll()) != null) {
        	assertNull(t.get());
        	assertTrue(threads.contains(t));
        }

        for (WeakReference<Thread> r : threads) {
        	assertNull(r.get());
        }

    }
}
