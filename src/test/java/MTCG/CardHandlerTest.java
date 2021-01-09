package MTCG;

import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class CardHandlerTest {

    Connection con;

    @BeforeEach
    public void createDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
    }

    @Test
    void getCardsFailTest() throws JsonProcessingException, SQLException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/cards", """
                GET /cards HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        CardHandler cardHandler = new CardHandler();
        String string = cardHandler.getCards(requestContextUser, con);
        Assertions.assertEquals("[]", string);
    }

    @Test
    void getDeckFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/deck", """
                GET /deck HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        CardHandler cardHandler = new CardHandler();
        String string = cardHandler.getDeck(requestContextUser, con, 2);
        Assertions.assertEquals("[]", string);
    }

    @Test
    void setDeckFailTestNotEnoughCards() throws JsonProcessingException, SQLException {
        RequestContext requestContextUser = new RequestContext("PUT", "HTTP/1.1", "/deck", """
                PUT /deck HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r
                Content-Length: 120\r
                \r          
                ["aa9999a0-734c-49c6-8f4a-651864b14e62", "d6e9c720-9b5a-40c7-a6b2-bc34752e3463", "d60e23cf-2238-4d49-844f-c7589ee5342e"]\r""");
        CardHandler cardHandler = new CardHandler();
        boolean bool = cardHandler.setDeck(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void setDeckFailTestNotYourCards() throws JsonProcessingException, SQLException {
        RequestContext requestContextUser = new RequestContext("PUT", "HTTP/1.1", "/deck", """
                PUT /deck HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic kienboec-mtcgToken\r
                Content-Length: 120\r
                \r          
                ["aa9999a0-734c-49c6-8f4a-651864b14e62", "d6e9c720-9b5a-40c7-a6b2-bc34752e3463", "d60e23cf-2238-4d49-844f-c7589ee5342e", "02a9c76e-b17d-427f-9240-2dd49b0d3bfd"]\r""");
        CardHandler cardHandler = new CardHandler();
        boolean bool = cardHandler.setDeck(requestContextUser, con);
        Assertions.assertFalse(bool);
    }
}