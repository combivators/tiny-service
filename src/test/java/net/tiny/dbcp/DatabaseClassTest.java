package net.tiny.dbcp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@Database
public class DatabaseClassTest {

    @Test
    public void testOne() throws InterruptedException {
        Thread.sleep(100);
        assertTrue(true);
    }

    @Test
    public void testTwo() throws InterruptedException {
        Thread.sleep(50);
        assertTrue(true);
    }
}
