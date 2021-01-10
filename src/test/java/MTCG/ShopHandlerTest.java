package MTCG;

import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class ShopHandlerTest {

    Connection con;

    @BeforeEach
    public void createDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
    }

    @Test
    void buyPackageFailTest() throws SQLException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/transactions/packages", """
                POST /transactions/packages HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Authorization: Basic dummy-mtcgToken\r
                Content-Length: 0\r""");
        ShopHandler shopHandler = new ShopHandler();
        boolean bool = shopHandler.buyPackage(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void tradeFailTestDoesNotExist() throws SQLException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/tradings/1", """
                POST /tradings/1 HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Authorization: Basic dummy-mtcgToken\r
                Content-Length: 38\r
                \r
                "951e886a-0fbf-425d-8df5-af2ee4830d85"\r""");
        ShopHandler shopHandler = new ShopHandler();
        boolean bool = shopHandler.trade(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void tradeFailTestTradeYourself() throws SQLException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0", """
                POST /tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0 HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Authorization: Basic kienboec-mtcgToken\r
                Content-Length: 38\r
                \r
                "4ec8b269-0dfa-4f97-809a-2c63fe2a0025"\r""");
        ShopHandler shopHandler = new ShopHandler();
        boolean bool = shopHandler.trade(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void tradeFailTestNotYourCard() throws SQLException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0", """
                POST /tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0 HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Authorization: Basic dummy-mtcgToken\r
                Content-Length: 38\r
                \r
                "4ec8b269-0dfa-4f97-809a-2c63fe2a0025"\r""");
        ShopHandler shopHandler = new ShopHandler();
        boolean bool = shopHandler.trade(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void tradeFailTestRequirementsNotMet() throws SQLException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0", """
                POST /tradings/6cd85277-4590-49d4-b0cf-ba0a921faad0 HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Authorization: Basic altenhof-mtcgToken\r
                Content-Length: 38\r
                \r
                "70962948-2bf7-44a9-9ded-8c68eeac7793"\r""");
        ShopHandler shopHandler = new ShopHandler();
        boolean bool = shopHandler.trade(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void getTradesFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/tradings", """
                GET /tradings HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        ShopHandler shopHandler = new ShopHandler();
        String string = shopHandler.getTrades(requestContextUser, con);
        Assertions.assertNull(string);
    }
}