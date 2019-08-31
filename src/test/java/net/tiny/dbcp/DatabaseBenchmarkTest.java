package net.tiny.dbcp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.benchmark.Benchmark;

@Database
public class DatabaseBenchmarkTest {

    static SimpleDataSource dataSource;
    static AtomicInteger total;
    static final int threadCount = 10;
    static String name;
    static long start;
/*
    @BeforeEach
    public void beforeEach() throws Exception {

        dataSource = h2database();
        start = System.currentTimeMillis();
    }

    @AfterEach
    public void afterEach() throws Exception {
        long end = System.currentTimeMillis();
        System.out.println(name + " Takes time " + (end - start) + "ms");
        dataSource.close();
        System.gc();
    }
*/
    static SimpleDataSource h2database() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:tcp://localhost:9001/h2")
          .username("sa")
          .password("");
        return ds;
    }

    // 100 ms
    @Benchmark()
    //@Test
    public void testGetConnectonBench() throws Exception {
        dataSource = h2database();
        total = new AtomicInteger(200 * 1000);
        start = System.currentTimeMillis();
        name = threadCount + " threads, " + total.get() + " times, [getConnection => close]";
        final CountDownLatch latch = new CountDownLatch(threadCount);
        for (int j = 0; j < threadCount; j++) {
            new Thread(new Runnable() {
                public void run() {
                    while (total.decrementAndGet() > 0) {
                        try {
                            Connection con = dataSource.getConnection();
                            con.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println(name + " Takes time " + (end - start) + "ms");
        dataSource.close();
    }
}
