package net.tiny.ws;


import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ServerRepositoryTest {

    @Test
    public void testCreateSessionKey() {
        ServerRepository repository = new ServerRepository();
        String session = repository.createSession().getId();
        System.out.println("Session: " + session);
        for (int i=0; i < 100; i++) {
            assertEquals(true, repository.createSession().getId().matches("[0-9a-z]+"));
        }
    }

    @Test
    public void testSessionAttribute() {
        ServerRepository repository = new ServerRepository();
        ServerSession session = repository.createSession();
        assertNotNull(session);
        session.setAttribute("user", "Hoge");
        session.setAttribute("token", Integer.valueOf(1234567));
        Enumeration<String> names = session.getAttributeNames();
        List<String> keys = Arrays.asList(names.nextElement(), names.nextElement());
        assertEquals(2, keys.size());

        assertEquals("Hoge", session.getAttribute("user"));
        assertEquals(Integer.valueOf(1234567), session.getAttribute("token"));
    }

    @Test
    public void testSessionTimeout() throws Exception {
        ServerRepository repository = new ServerRepository();
        repository.setSessionTimeout(1000L);
        ServerSession session = repository.createSession();
        session.setAttribute("user", "Hoge");
        assertTrue(session.isValid());
        String id = session.getId();
        Thread.sleep(1100L);
        assertFalse(session.isValid());

        session.keepAlive();
        assertTrue(session.isValid());

        session = repository.getSession(id).get();
        assertTrue(session.isValid());
        Thread.sleep(1100L);
        assertFalse(repository.getSession(id).isPresent());

    }
}
