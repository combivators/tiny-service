package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class DatabasedTest {

    @Test
    public void testNormalCase() throws InterruptedException {
        Thread.sleep(10);
    }

    @Database
    public void testDatabasedOne() throws InterruptedException {
        Thread.sleep(100);
        assertTrue(true);
        System.out.println("testDatabasedOne");
    }

    @Database
    public void testDatabasedTwo() throws InterruptedException {
        Thread.sleep(200);
        System.out.println("testDatabasedTwo");
        assertTrue(true);
    }

}
