package net.tiny.dbcp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.dbcp.SimpleDataSource;
import net.tiny.unit.db.Database;

@Database
public class PerfBenchmarkTest {

    static DataSource dataSource;
    static AtomicInteger total;
    static final int threadCount = 10;
    static String name;
    static long start;
    static CountDownLatch latch;

    @BeforeEach
    public void beforeEach() throws Exception {
        total = new AtomicInteger(200 * 1000);
        dataSource = h2database();
        start = System.currentTimeMillis();
        latch = new CountDownLatch(threadCount);
    }

    @AfterEach
    public void afterEach() throws Exception {
        long end = System.currentTimeMillis();
        System.out.println(name + " Takes time " + (end - start) + "ms");
        dataSource = null;
        System.gc();
    }

    static DataSource h2database() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:tcp://localhost:9001/h2")
          .username("sa")
          .password("");
        return ds;
    }

    // H2:100ms  Oracle:1466ms
    @Test
    public void testGetConnectonBench() throws InterruptedException {
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
    }

    // 5619 ms
    @Test
    public void testStatementBench() throws Exception {
        name = threadCount + " threads, " + total.get()
                + " times, [getConnection => Statement(select 1) => close]";

        for (int j = 0; j < threadCount; j++) {
            new Thread(new Runnable() {
                public void run() {
                    while (total.decrementAndGet() > 0) {
                        try {
                            Connection con = dataSource.getConnection();

                            Statement stat = con.createStatement();
                            ResultSet rs = stat.executeQuery("select 1");
                            rs.close();
                            stat.close();
                            con.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    latch.countDown();
                }
            }).start();
        }
        latch.await(); // wait threads finish job
    }

    // H2:5264ms  Oracle:1466ms
    @Test
    public void testPrepareStatementBench() throws InterruptedException {
        name = threadCount + " threads, " + total.get()
                + " times, [getConnection => PreparedStatement(select 1) => close]";

        for (int j = 0; j < threadCount; j++) {
            new Thread(new Runnable() {
                public void run() {
                    while (total.decrementAndGet() > 0) {
                        try {
                            Connection con = dataSource.getConnection();
                            PreparedStatement ps = con.prepareStatement("select 1");
                            ResultSet rs = ps.executeQuery();
                            rs.close();
                            ps.close();
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
    }
}
