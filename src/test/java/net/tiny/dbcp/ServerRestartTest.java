package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

public class ServerRestartTest {

    static DataSource h2database() {
        SimpleDataSource ds = new SimpleDataSource();
        ds.getBuilder()
          .driver("org.h2.Driver")
          .url("jdbc:h2:tcp://localhost:9001/h2")
          .username("sa")
          .password("");
        return ds;
    }


    @Test
    public void testServerRestart() throws Exception {

        H2Engine engine = H2Engine.getEngine();
        engine.start();

        final int threads = 10;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);

        DataSource ds = h2database();
        Task task = new Task(ds);
        task.run();
        assertNull(task.lastError);

        for (int i = 0; i < threads * 100; i++) {
            pool.submit(new Task(ds));
        }

        Thread.sleep(1000);
        System.out.println(ds);

        // for h2 restart
        engine.stop();
        Thread.sleep(500);
        engine = H2Engine.getEngine();
        engine.start();

        new Task(ds).run();
        new Task(ds).run();
        Thread.sleep(100);

        for (int i = 0; i < threads * 100; i++) {
            pool.submit(new Task(ds));
        }

        pool.shutdown();
        pool.awaitTermination(100, TimeUnit.SECONDS);
        ds = null;
        System.gc();


        engine.stop();
    }

    static class Task implements Runnable {
        private final DataSource ds;
        Throwable lastError;

        public Task(DataSource ds) {
            this.ds = ds;
        }

        public void run() {
            try {
                Connection con = ds.getConnection();
                Statement statement = con.createStatement();
                ResultSet rs = statement.executeQuery("SELECT 1");
                while (rs.next()) {
                    String s = rs.getString(1);
                    // System.out.println(s);
                }
            } catch (RuntimeException e) {
                lastError = e;
            } catch (SQLException e) {
                lastError = e;
                System.out.println(ds + "\t" + e);
            }
        }
    }
}