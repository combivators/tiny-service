package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class UniqueTest {

    @Test
    public void testUniqueKey() throws Exception {
        for(int i =0; i<20; i++) {
            String key = Unique.uniqueKey();
            System.out.println(key);
        }
        List<String> list = new ArrayList<String>();
        for(int i =0; i<2000; i++) {
            String key = Unique.uniqueKey();
            assertFalse(list.contains(key));
            list.add(key);
        }
    }

    @Test
    public void testMultiUnique() throws Exception {
        UniqueTask[] tasks = new UniqueTask[5];
        for(int i=0; i<tasks.length; i++) {
            tasks[i] = new UniqueTask();
        }
        ThreadGroup group = new ThreadGroup("Unique");
        for(int i=0; i<tasks.length; i++) {
            Thread thread = new Thread(group, tasks[i]);
            thread.start();
        }
        while(group.activeCount()>0) {
            Thread.sleep(500L);
        }
        List<String> list = new ArrayList<String>();
        for(int i=0; i<tasks.length; i++) {
            for(String key : tasks[i].list) {
                assertFalse(list.contains(key));
                list.add(key);
            }
        }
    }

    static class UniqueTask implements Runnable {
        List<String> list = new ArrayList<String>();
        @Override
        public void run() {
            for(int i =0; i<2000; i++) {
                String key = Unique.uniqueKey();
                assertFalse(list.contains(key));
                list.add(key);
            }
        }
    }
}
