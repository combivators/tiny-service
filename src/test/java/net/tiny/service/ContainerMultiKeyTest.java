package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContainerMultiKeyTest {

    @Test
    public void testNullKeys() throws Exception {
    	try {
    		Container.MultiKey nullKey = new Container.MultiKey();
    		fail(nullKey.toString());
    	} catch (Exception e) {
    		assertTrue(e instanceof IllegalArgumentException);
    	}
    }

    @Test
    public void testCompare() throws Exception {
    	Container.MultiKey key = new Container.MultiKey("one", "two", "three", new Integer(100), Container.MultiKey.class);
    	Container.MultiKey other = new Container.MultiKey("one", null, new Integer(100), "two", "two", Container.MultiKey.class, "three");
    	assertEquals(key.hashCode(), other.hashCode());
    	assertEquals(key, other);

		List<Object> keys = new ArrayList<>();
		keys.add("one");
		keys.add(new Integer(100));
		keys.add(new Integer(100));
		keys.add("two");
		keys.add("two");
		keys.add("three");
		keys.add(Container.MultiKey.class);
      	Container.MultiKey list = new Container.MultiKey(keys);
    	assertEquals(key.hashCode(), list.hashCode());
    	assertEquals(key, list);

    	Container.MultiKey sub = new Container.MultiKey("two", new Integer(200), Container.MultiKey.class);
    	assertTrue(key.contains(sub));

    	Container.MultiKey unkown = new Container.MultiKey(new Integer(200), String.class);
    	assertFalse(key.contains(unkown));
    }

    @Test
    public void testGetterSetter() throws Exception {
    	Container.MultiKey key = new Container.MultiKey("one", "two", "three", new Integer(100), Container.MultiKey.class);
    	assertEquals(5, key.size());
    	assertEquals("one", key.getKey(0));
    	assertEquals("three", key.getKey(2));
    	assertEquals(new Integer(100), key.getKey(3));

    	assertTrue(key.contains("two"));
    	assertTrue(key.contains(new Integer(100)));
    	assertTrue(key.contains(Container.MultiKey.class));

    	assertFalse(key.contains(null));
    	assertFalse(key.contains("ten"));
    	assertFalse(key.contains(new Integer(10)));
    	assertFalse(key.contains(String.class));
    	assertFalse(key.contains(Object.class));

    	assertEquals(Container.MultiKey.class, key.getKey(4));
    	key.getKeys().forEach(k -> System.out.print(String.valueOf(k) + " "));
    	System.out.println();
    }

    @Test
    public void testMerge() throws Exception {
    	Container.MultiKey source = new Container.MultiKey("one", "two", "three", new Integer(100), Container.MultiKey.class);
    	int hashCode1 = source.hashCode();
    	assertEquals(5, source.size());
    	Container.MultiKey other = source.merge(Arrays.asList("four", new Integer(100)));
    	assertEquals(6, other.size());
    	assertNotEquals(hashCode1, other.hashCode());
    }
}
