package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class PostParameterPaserTest {

    @Test
    public void testParseQuery() throws Exception {
        final Map<String, Object> args = new HashMap<>();
        String query = "singleRange=65&doubleRange=300%3B700&dateRange=&modernRange=30&sharpRange=80&squareRange=80";
        PostParameterPaser.parseQuery(query, args);
        assertEquals("300;700", args.get("doubleRange"));
        assertEquals("65", args.get("singleRange"));
        assertEquals("30", args.get("modernRange"));
        assertEquals("", args.get("dateRange"));
        assertEquals("80", args.get("sharpRange"));
        assertEquals("80", args.get("squareRange"));
    }
}
