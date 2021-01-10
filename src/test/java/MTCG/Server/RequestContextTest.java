package MTCG.Server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RequestContextTest {

    @Test public void testConstructor(){
        RequestContext requestContextPOST = new RequestContext("POST", "HTTP/1.1", "/users", "POST /users HTTP/1.1\r\nHost: localhost:10001\r\nUser-Agent: insomnia/2020.4.2\r\nAccept: */*\r\nContent-Length: 5\r\n\r\nTest2\r\n");
        Assertions.assertEquals("POST", requestContextPOST.getRequest());
        Assertions.assertEquals("HTTP/1.1", requestContextPOST.getHttpType());
        Assertions.assertEquals("/users", requestContextPOST.getURI());
        Assertions.assertEquals("Test2", requestContextPOST.getMessage());

        RequestContext requestContextGET = new RequestContext("GET", "HTTP/1.1", "/log", "POST /log HTTP/1.1\r\nHost: localhost:10001\r\nUser-Agent: insomnia/2020.4.2\r\nAccept: */*\r\nContent-Length: 5\r\n\r\nTest2\r\n");
        Assertions.assertNull(requestContextGET.getMessage());
    }
}