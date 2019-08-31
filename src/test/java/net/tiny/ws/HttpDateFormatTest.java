package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class HttpDateFormatTest {

    @Test
    public void testFormatParse() throws Exception {
        Date date = new Date(1561068712300L);
        assertEquals("Thu, 20 Jun 2019 22:11:52 GMT", HttpDateFormat.format(date));
        assertEquals(HttpDateFormat.format(new Date(System.currentTimeMillis())),
                HttpDateFormat.formatCurrentDate());

        //Error less than 1 second
        assertNotEquals(date.getTime(), HttpDateFormat.parse("Thu, 20 Jun 2019 22:11:52 GMT").getTime());
        assertTrue(Math.abs(date.getTime() - HttpDateFormat.parse("Thu, 20 Jun 2019 22:11:52 GMT").getTime()) < 1000L);
    }

    @Test
    public void testMultipleFormatParse() throws Exception {
        //Warm up
        System.out.print("Warm up");
        for(int i=0; i<2; i++) {
            doMultiple(true, true);
            doMultiple(false, false);
            System.out.print(".");
        }
        System.out.println();
        System.out.print("Multi format test");
        long synTime = 0L;
        long queTime = 0L;
        for(int i=0; i<10; i++) {
            synTime += doMultiple(true, true);
            queTime += doMultiple(false, true);
            System.out.print(".");
        }
        System.out.println();
        System.out.println(String.format("Format Synchronized:%dms Blocking Queue: %dms", synTime, queTime));

        System.out.print("Multi parse test");
        synTime = 0L;
        queTime = 0L;
        for(int i=0; i<10; i++) {
            synTime += doMultiple(true, false);
            queTime += doMultiple(false, false);
            System.out.print(".");
        }
        System.out.println();
        System.out.println(String.format("Parse Synchronized:%dms Blocking Queue: %dms", synTime, queTime));

    }

    long doMultiple(boolean syn, boolean f) throws Exception {
        int num = 10;
        FormatTask[] tasks = new FormatTask[num];
        for(int i=0; i<tasks.length; i++) {
            tasks[i] = new FormatTask(f);
        }
        ThreadGroup group = new ThreadGroup("Test");
        for(int i=0; i<tasks.length; i++) {
            Thread thread = new Thread(group, tasks[i]);
            thread.start();
        }
        while(group.activeCount()>0) {
            Thread.sleep(500L);
        }
        HttpDateFormat.synchronize = syn;
        long total = 0L;
        for(int i=0; i<tasks.length; i++) {
            total += tasks[i].time;
        }
        //System.out.println(String.format("Synchronize:%s Multiple: %d total %dms", syn, num, total));
        return total;
    }

    static class FormatTask implements Runnable {
        String text = "Thu, 20 Jun 2019 22:11:52 GMT";
        long time = 0L;
        boolean flag = true;
        FormatTask(boolean f) {
            flag = f;
        }
        @Override
        public void run() {
            long st = System.currentTimeMillis();
            for(int i =0; i<2000; i++) {
                if (flag) {
                    HttpDateFormat.formatCurrentDate();
                } else {
                    HttpDateFormat.parse(text);
                }
            }
            time = System.currentTimeMillis() - st;
        }
    }
}
