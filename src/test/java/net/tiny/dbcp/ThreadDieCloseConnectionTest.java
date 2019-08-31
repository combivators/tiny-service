package net.tiny.dbcp;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ThreadDieCloseConnectionTest {


    static H2Engine engine;

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        engine = H2Engine.getEngine();
        engine.start();
    }

    @AfterAll
    public static void afterAll() throws Exception {
        engine.clearDatabase(true);
        engine.stop();
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

    @Test
    public void testConnectionClosedAfterThreadDired() throws Exception {
        DataSource dataSource = h2database();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        Connection con = dataSource.getConnection();
                        con.getMetaData().getDatabaseMajorVersion();
                    } catch (SQLException e) {
                    }
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        threads.clear();
        threads = null;

        for (int i = 0; i < 10; i++) {
            System.out.println(dataSource);
            System.gc();
            Thread.sleep(100);
            dataSource.getConnection();
        }

        ((SimpleDataSource)dataSource).close();
        System.gc();
    }
}
