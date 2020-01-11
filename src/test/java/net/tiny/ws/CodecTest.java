package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class CodecTest {

    @Test
    public void testHexEncodeDecode() throws Exception {
        assertEquals("31323334353658595a616263", Codec.encodeHex("123456XYZabc".getBytes()));
        assertEquals("123456XYZabc", new String(Codec.decodeHex("31323334353658595a616263")));
    }

    @Test
    public void testHashPassword() throws Exception {
        assertEquals("35a7757596af7c3c020a5a47559d75e55956e730c52830be0fddf12195612a5ae6d0f2256584bd5352888287ee99a05e",
                Codec.hash("123456XYZabc"));
    }
}
