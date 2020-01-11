package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PatternsTest {

    String[] vaild = new String[] {
        "/",
        "/500.vm",
        "/404.vm",
        "/html/abcdef.vm",
        "/html/topic.vm?sid=1&fid=1&tid=1",
        "/html/act_1_1_1.html",
        "/html/forum/topic.do?sid=1&fid=1&tid=1",
        "/html/images/act_1_1_1.html",
        "/wap/abcdef.vm",
        "/wap/topic.vm?sid=1&fid=1&tid=1",
        "/wap/act_1_1_1.html",
        "/wap/forum/topic.do?sid=1&fid=1&tid=1",
        "/wap/images/act_1_1_1.html",
    };

    String[] invaild = new String[] {
        "/images/wap.gif",
        "/images/style/html.png",
        "/images/user/topic.jpg",
        "/js/function.js",
        "/js/fcedit/f.js",
        "/styles/body.css",
        "/styles/menu/1.css",
        "/css/body.css",
        "/css/menu/1.css",
        "/jws/body.jar",
        "/jws/lib/1.css",
        "/flash/body.swf",
        "/flash/movies/1.swf",
        "/uploads/body.doc",
        "/uploads/word/1.pdf",
        "/userfiles/body.jpg",
        "/userfiles/word/1.gif",
    };

    String[] other = new String[] {
            "/html/wap.gif",
            "/wap/style/html.png",
    };

    @Test
    public void testVaildInvaild() {
        Patterns patterns = new Patterns("");
        for(int i=0; i<vaild.length; i++) {
            assertTrue(patterns.vaild(vaild[i]), "# " + vaild[i]);
        }
        for(int i=0; i<invaild.length; i++) {
            assertTrue(patterns.vaild(invaild[i]), "# " + invaild[i]);
        }

        String param = "/images/.*, /js/.*, /styles/.*, /jws/.*, /flash/.*, /css/.*, /uploads/.*, /userfiles/.*";
        patterns.setExclude(param);

        for(int i=0; i<vaild.length; i++) {
            assertFalse(patterns.invaild(vaild[i]), "# " + invaild[i]);
        }
        for(int i=0; i<invaild.length; i++) {
            assertTrue( patterns.invaild(invaild[i]), "# " + invaild[i]);
        }
        for(int i=0; i<vaild.length; i++) {
            assertTrue(patterns.vaild(vaild[i]), "# " + invaild[i]);
        }
        for(int i=0; i<invaild.length; i++) {
            assertFalse(patterns.vaild(invaild[i]), "# " + invaild[i]);
        }

        patterns = new Patterns(".*[.]vm");
        assertTrue(patterns.vaild("web/500.vm"));
        assertFalse(patterns.vaild("web/500.tvm"));
        assertFalse(patterns.vaild("web/500.vmt"));

        patterns = new Patterns(".*/WEB-INF/.*[.]xml");
        patterns.setExclude(".*/META-INF/.*");
        assertTrue(patterns.vaild("/src/main/webapp/WEB-INF/classes/messages.xml"));
        assertTrue(patterns.vaild("/src/main/webapp/WEB-INF/classes/conf/sample.xml"));

        assertFalse(patterns.vaild("/target/test-classes/META-INF/context.xml"));
        assertFalse(patterns.vaild("/src/main/webapp/WEB-INF/classes/META-INF/beans.xml"));
        assertFalse(patterns.vaild("/target/test-classes/example.xml"));
    }
}
