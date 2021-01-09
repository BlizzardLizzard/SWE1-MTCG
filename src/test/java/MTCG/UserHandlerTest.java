package MTCG;

import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

class UserHandlerTest {
    Connection con;

    @BeforeEach
    public void createDatabase() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
    }


    @Test
    void createUserFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/users", """
                POST /users HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daniel"}""");
        UserHandler userHandler = new UserHandler();
        boolean user = userHandler.createUser(requestContextUser, con);
        Assertions.assertFalse(user);
    }

    @Test
    void loginUserSuccessTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/sessions", """
                POST /sessions HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daniel"}""");
        UserHandler userHandler = new UserHandler();
        boolean user = userHandler.loginUser(requestContextUser, con);
        Assertions.assertTrue(user);
    }

    @Test
    void loginUserFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("POST", "HTTP/1.1", "/sessions", """
                POST /sessions HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Content-Type: application/json\r
                Content-Length: 44\r
                \r
                {"Username":"kienboec", "Password":"daiel"}""");
        UserHandler userHandler = new UserHandler();
        boolean user = userHandler.loginUser(requestContextUser, con);
        Assertions.assertFalse(user);
    }

    @Test
    void getUserDataFailTest() throws SQLException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/users/kienboec", """
                GET /users/kienboec HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        UserHandler userHandler = new UserHandler();
        ResultSet resultSet = userHandler.getUserData(requestContextUser, con);
        Assertions.assertNull(resultSet);
    }

    @Test
    void setUserDataFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("PUT", "HTTP/1.1", "/users/kienboec", """
              PUT /users/kienboec HTTP/1.1\r
              Host: localhost:10001\r
              User-Agent: curl/7.71.1\r
              Accept: */*\r
              Content-Type: application/json\r
              Authorization: Basic dummy-mtcgToken\r
              Content-Length: 61\r
              \r
              {"Name": "Kienboeck",  "Bio": "me playin...", "Image": ":-)"}\r""");
        UserHandler userHandler = new UserHandler();
        boolean bool = userHandler.setUserData(requestContextUser, con);
        Assertions.assertFalse(bool);
    }

    @Test
    void getStatsFailTest() throws SQLException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/stats", """
                GET /stats HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        UserHandler userHandler = new UserHandler();
        ResultSet resultSet = userHandler.getStats(requestContextUser, con);
        Assertions.assertNull(resultSet);
    }

    @Test
    void getScoreFailTest() throws SQLException, JsonProcessingException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/score", """
                GET /score HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        UserHandler userHandler = new UserHandler();
        String string = userHandler.getScore(requestContextUser, con);
        Assertions.assertNull(string);
    }

    @Test
    void getBattleLogsFailTestEmptyReply() throws FileNotFoundException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/logs", """
                GET /logs HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r
                Authorization: Basic dummy-mtcgToken\r""");
        UserHandler userHandler = new UserHandler();
        String string = userHandler.getBattleLogs(requestContextUser);
        Assertions.assertNull(string);
    }

    @Test
    void getBattleLogsFailNoToken() throws FileNotFoundException {
        RequestContext requestContextUser = new RequestContext("GET", "HTTP/1.1", "/logs", """
                GET /logs HTTP/1.1\r
                Host: localhost:10001\r
                User-Agent: curl/7.71.1\r
                Accept: */*\r""");
        UserHandler userHandler = new UserHandler();
        String string = userHandler.getBattleLogs(requestContextUser);
        Assertions.assertNull(string);
    }
}