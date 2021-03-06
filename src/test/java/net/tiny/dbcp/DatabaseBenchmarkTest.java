package net.tiny.dbcp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import net.tiny.benchmark.Benchmark;
import net.tiny.unit.db.Database;

@Database
public class DatabaseBenchmarkTest {

    static SimpleDataSource dataSource;
    static AtomicInteger total;
    static final int threadCount = 10;
    static String name;
    static long start;

    static SimpleDataSource h2database() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:tcp://localhost:9092/h2")
          .username("sa")
          .password("");
        return ds;
    }

    // 100 ms
    @Benchmark()
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
