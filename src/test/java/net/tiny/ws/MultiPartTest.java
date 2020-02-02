package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.tiny.ws.FormDataHandler.MultiPart;
import net.tiny.ws.FormDataHandler.PartType;

public class MultiPartTest {

    @Test
    public void testSearchBytes() throws Exception {
        String boundary = "--WebKitFormBoundaryKBRUiUWrIpW9wq2j";
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        String contents =
                "----WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"textline\"\r\n" +
                "Content-type: text/plain;charset=UTF-8\r\n" +
                "\r\n" +
                "value of textfield here\r\n" +
                "----WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"datafile\"; filename=\"test.txt\"\r\n" +
                "Content-type: application/octet-stream\r\n" +
                "\r\n" +
                "1234567890\r\n" +
                "abcdefghijk\r\n" +
                "----WebKitFormBoundaryKBRUiUWrIpW9wq2j--\r\n" +
                "";

        List<Integer> offsets = FormDataHandler.searchBytes(contents.getBytes(), boundaryBytes, 0, contents.length() -1);
        int startPart = offsets.get(0);
        int endPart = contents.length();
        assertEquals(3, offsets.size());
        assertEquals(0, startPart);
        assertEquals(375, endPart);

    }

    @Test
    public void testParse() throws Exception {
        String boundary = "----WebKitFormBoundarymjRB3jnJWvb0lJyw";
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        String contents =
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw\r\n" +
                "Content-Disposition: form-data; name=\"input-b[]\"; filename=\"E01.txt\"\r\n" +
                "Content-type: text/plain;charset=UTF-8\r\n" +
                "\r\n" +
                "The first value of textfield here\r\n" +
                "The last value of textfield here\r\n" +
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw\r\n" +
                "Content-Disposition: form-data; name=\"fileId\"\r\n" +
                "\r\n" +
                "370_E01.txt\r\n" +
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw\r\n" +
                "Content-Disposition: form-data; name=\"initialPreview\"\r\n" +
                "\r\n" +
                "[]\r\n" +
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw\r\n" +
                "Content-Disposition: form-data; name=\"initialPreviewConfig\"\r\n" +
                "\r\n" +
                "[]\r\n" +
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw\r\n" +
                "Content-Disposition: form-data; name=\"initialPreviewThumbTags\"\r\n" +
                "\r\n" +
                "[]\r\n" +
                "------WebKitFormBoundarymjRB3jnJWvb0lJyw--\r\n" +
                "";
        final byte[] payload = contents.getBytes();
        List<Integer> offsets = FormDataHandler.searchBytes(payload, boundaryBytes, 0, payload.length-1);
        int startPart = offsets.get(0);
        int endPart = contents.length();
        assertEquals(6, offsets.size());
        assertEquals(0, startPart);
        assertEquals(695, endPart);

        List<MultiPart> parts = FormDataHandler.parse(payload, boundaryBytes);
        assertEquals(5, parts.size());

        MultiPart mp = parts.get(0);
        assertEquals("input-b[]", mp.name);
        assertEquals(PartType.FILE, mp.type);
        assertNull(mp.value);
        assertEquals("The first value of textfield here\r\nThe last value of textfield here", new String(mp.bytes, StandardCharsets.UTF_8));
        assertEquals(67, mp.bytes.length);

        mp = parts.get(1);
        assertEquals("fileId", mp.name);
        assertEquals(PartType.TEXT, mp.type);
        assertEquals("370_E01.txt", mp.value);
        assertNull(mp.bytes);

        for (MultiPart part : parts) {
            System.out.println("MultiPart: " + part.toString());
        }
    }


}
