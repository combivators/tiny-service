package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

public class ContainerTest {

    @Test
    public void testSetGetBean() throws Exception {
        Container container = new Container();
        One one = new One();

        //Default key is class name
        container.setBean("one", one);
        assertEquals(1, container.size());
        assertEquals(1, container.size(true));
        assertEquals(0, container.size(false));
        assertEquals(one, container.getBean("One"));
        assertEquals(one, container.getBean(One.class));
        assertEquals(one, container.getBean(DummyZero.class));
        assertEquals(one, container.getBean(DummyOne.class));
        assertNull(container.getBean(Serializable.class));
        Container.MultiKey key = container.getMultiKey(One.class);
        assertEquals(5, key.size());
        //assertNotEquals(two, container.getBean("two"));

        assertTrue(container.contains("One"));
        assertTrue(container.contains(One.class));
        assertTrue(container.contains(DummyZero.class));
        assertTrue(container.contains(DummyOne.class));
        //assertEquals(1, container.getAllKeys().length);

        assertFalse(container.contains("this"));

        container.setBean("this", one);
        assertTrue(container.contains("this"));

        assertEquals(one, container.getBean("this"));
        assertEquals(1, container.size());

        key = container.getMultiKey("this");
        assertNotNull(key);
        assertEquals(6, key.size());

        container.removeBean("this");
        assertEquals(0, container.size());
        assertEquals(0, container.size(true));
        assertEquals(0, container.size(false));
        assertFalse(container.contains("one"));
        assertFalse(container.contains(One.class));
    }

    @Test
    public void testSetGetBeansByInterface() throws Exception {
        Container container = new Container();
        assertEquals(0, container.size());
        assertEquals(0, container.size(true));
        assertEquals(0, container.size(false));

        One one = new One();
        Two two = new Two();

        container.setBean("one", one);
        container.setBean("two", two);
        assertEquals("Container(singleton:2, classes:0)", container.toString());

        assertEquals(2, container.size());
        assertEquals(2, container.size(true));//singleton
        assertNull(container.getBean(Serializable.class));
        assertEquals(one, container.getBean(One.class));
        assertEquals(two, container.getBean(Two.class));

        assertEquals(one, container.getBean(DummyOne.class));
        Object bean = container.getBean(DummyZero.class);
        assertNotNull(bean);

        DummyZero zero = container.getBean("Two", DummyZero.class);
        assertEquals(two, zero);
        zero = container.getBean("One", DummyZero.class);
        assertEquals(one, zero);

        List<DummyZero> list = container.getBeans(DummyZero.class);
        assertEquals(2, list.size());

        String[] names = container.getAllKeyNames();
        assertEquals(4, names.length);

        Class<?>[] types = container.getAllKeyTypes();
        assertEquals(5, types.length);

        Class<?>[] faces = container.getAllIntefaceKeys();
        assertEquals(3, faces.length);
    }

    @Test
    public void testSetBeanByName() throws Exception {
        Container container = new Container();
        One one = new One();
        container.setBean("one", one);
        String[] names = container.getAllKeyNames();
        assertEquals(2, names.length);

        Class<?>[] types = container.getAllKeyTypes();
        assertEquals(3, types.length);

        Class<?>[] faces = container.getAllIntefaceKeys();
        assertEquals(2, faces.length);
    }

    @Test
    public void testSetJavaTypeByName() throws Exception {
        Container container = new Container();
        container.setBean("one", new Integer(1));
        container.setBean("Two", "Two");
        String[] names = container.getAllKeyNames();
        assertEquals(2, names.length);

    }

    public static interface DummyZero {
    	String getName();
    }

    public static interface DummyOne {
    	void setName(String name);
    }

    public static class One implements DummyZero, DummyOne, Serializable  {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Two implements DummyZero, Serializable {
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Three implements DummyOne, Serializable {
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Four {
        private String name;

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}
