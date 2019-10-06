package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.Constants.HTTP_METHOD;


public class BaseWebServiceTest {

    @Test
    public void testIsAllowedMethod() {
        BaseWebService handler = new AllAllowedHandler();
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.GET));
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.POST));
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.PUT));
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.DELETE));
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.OPTIONS));

        handler = new GetOnlyHandler();
        assertTrue(handler.isAllowedMethod(HTTP_METHOD.GET));
        assertFalse(handler.isAllowedMethod(HTTP_METHOD.POST));
        assertFalse(handler.isAllowedMethod(HTTP_METHOD.PUT));
        assertFalse(handler.isAllowedMethod(HTTP_METHOD.DELETE));
    }

    static class AllAllowedHandler extends BaseWebService {
        @Override
        protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        }
    }

    static class GetOnlyHandler extends BaseWebService {
        @Override
        protected boolean doGetOnly() {
            return true;
        }
        @Override
        protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        }
    }
}
