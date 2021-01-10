package MTCG;

import MTCG.JsonObjects.Scoreboard;
import MTCG.Server.ReplyHandler;
import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class UserHandler {
    public String URI;
    public String request;
    public boolean requestHandeled;
    public int specialCaseInt;

    //takes care of all the incoming requests and directs them to the right response
    public UserHandler(RequestContext requestContext, Socket socket) throws JsonProcessingException, SQLException, ClassNotFoundException, FileNotFoundException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
        this.URI = requestContext.URI;
        this.request = requestContext.request;
        ReplyHandler replyHandler = new ReplyHandler(socket);
        if(URI.startsWith("/users")) {
            if (request.equals("POST")) {
                requestHandeled = createUser(requestContext, con);
                if (requestHandeled) {
                    replyHandler.userCreated();
                } else {
                    replyHandler.userAlreadyExists();
                }
            }
            if(request.equals("GET")){
                ResultSet resultSet = getUserData(requestContext, con);
                if(requestHandeled){
                    replyHandler.userData(resultSet);
                }else{
                    replyHandler.userWrongToken();
                }
            }
            if(request.equals("PUT")){
                requestHandeled = setUserData(requestContext, con);
                if(requestHandeled){
                    replyHandler.setUserInfo();
                }else{
                    replyHandler.userWrongToken();
                }
            }
        }
        if(URI.equals("/sessions")){
            requestHandeled = loginUser(requestContext, con);
            if(requestHandeled){
                replyHandler.userLoggedIn();
            }else{
                replyHandler.userFailedLogin();
            }
        }
        if(URI.equals("/stats")){
            ResultSet stats = getStats(requestContext, con);
            if(requestHandeled){
                replyHandler.getStats(stats);
            }else{
                replyHandler.userWrongToken();
            }
        }
        if(URI.equals("/score")){
            String message = getScore(requestContext, con);
            if(requestHandeled){
                replyHandler.getScore(message);
            }else{
                replyHandler.userWrongToken();
            }
        }
        if(URI.equals("/log")){
           String message = getBattleLogs(requestContext);
           if(message != null){
               replyHandler.getLogs(message);
           }else if(specialCaseInt == 1){
               //user has no battles under his belt
               replyHandler.noLogEntries();
           }else if(specialCaseInt == 2){
               //there was no token in the request
                replyHandler.noToken();
           }
        }
    }

    public UserHandler(){
    }

    //creates the user
    public boolean createUser(RequestContext requestContext, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        String username = jsonNode.get("Username").asText();
        String password = jsonNode.get("Password").asText();
        PreparedStatement count = con.prepareStatement("SELECT count(*) AS total FROM users WHERE username = ?");
        count.setString(1, username);
        ResultSet resultSet = count.executeQuery();
        if (resultSet.next()) {
            int rows = resultSet.getInt(1);
            //checks if user already exists if not creates a new user
            if (rows == 0) {
                PreparedStatement pst = con.prepareStatement("INSERT INTO users(username, password) VALUES(?,?) ");
                pst.setString(1, username);
                pst.setString(2, password);
                pst.executeUpdate();
                return true;
            }
        }
        return false;
    }

    //logs in the user
    public boolean loginUser(RequestContext requestContext, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        String username = jsonNode.get("Username").asText();
        String password = jsonNode.get("Password").asText();
        PreparedStatement data = con.prepareStatement("SELECT * FROM users WHERE username = ?");
        data.setString(1, username);
        ResultSet resultSet = data.executeQuery();
        if (resultSet.next()) {
            //if password in database and password from json string equal log player in
            return password.equals(resultSet.getString(2));
        } else {
            return false;
        }
    }

    //get all data that the user has set in his profile
    public ResultSet getUserData(RequestContext requestContext, Connection con) throws SQLException {
        String[] user = requestContext.URI.split("/");
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        //checks if the name of the token and the name of the user are the same
        if(user[2].equals(tokenName[0])){
            PreparedStatement userBio = con.prepareStatement("SELECT name, bio, image FROM users WHERE username = ?");
            userBio.setString(1,user[2]);
            ResultSet userInfo = userBio.executeQuery();
            if(userInfo.next()){
                requestHandeled = true;
                return userInfo;
            }
        }
        requestHandeled = false;
        return null;
    }

    //put data that the users provides into his profile
    public boolean setUserData(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String[] user = requestContext.URI.split("/");
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        //checks if the name of the token and the name of the user are the same and sets the provided data into their profile
        if(user[2].equals(tokenName[0])){
            JsonNode jsonNode = mapper.readTree(requestContext.message);
            String name = jsonNode.get("Name").asText();
            String bio = jsonNode.get("Bio").asText();
            String image = jsonNode.get("Image").asText();
            PreparedStatement updateUser = con.prepareStatement("UPDATE users SET name = ?, bio = ?, image= ? WHERE username = ?");
            updateUser.setString(1, name);
            updateUser.setString(2, bio);
            updateUser.setString(3, image);
            updateUser.setString(4,user[2]);
            updateUser.executeUpdate();
            return true;
        }
        return false;
    }

    //replys with the stats of the player (elo, wins, losses, draws and gamesplayed)
    public ResultSet getStats(RequestContext requestContext, Connection con) throws SQLException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement stats = con.prepareStatement("SELECT elo, wins, losses, draws, gamesplayed FROM users WHERE username = ?");
        stats.setString(1,tokenName[0]);
        ResultSet resultStats = stats.executeQuery();
        if(resultStats.next()){
            requestHandeled = true;
            return resultStats;
        }
        requestHandeled = false;
        return null;
    }

    //shows the current scoreboard of all players which are registered
    public String getScore(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
        userExists.setString(1,tokenName[0]);
        ResultSet playerExists = userExists.executeQuery();
        if(playerExists.next()){
            int rows = playerExists.getInt(1);
            //checks if player that sent the request exists
            if(rows == 1){
                StringBuilder message = new StringBuilder();
                PreparedStatement score = con.prepareStatement("SELECT username, elo FROM users ORDER BY elo DESC");
                ResultSet scoreboard = score.executeQuery();
                ObjectMapper mapper = new ObjectMapper();
                message.append("[");
                    while(scoreboard.next()){
                        Scoreboard scoreboardClass = new Scoreboard(scoreboard.getString(1), scoreboard.getInt(2));
                        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(scoreboardClass);
                        message.append(json);
                        if(!scoreboard.isLast()) {
                            message.append(",");
                        }
                    }
                    message.append("]");
                    requestHandeled = true;
                    return message.toString();
                }
            }
        requestHandeled = false;
        return null;
    }

    //sends back all their previous battles that have been taken place
    public String getBattleLogs(RequestContext requestContext) throws FileNotFoundException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        //checks if a token has been sent in the request
        if (token != null) {
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");

            File getFiles = new File("battleLog/");
            String[] pathNames = getFiles.list();
            ArrayList<String> userLogs = new ArrayList<>();
            StringBuilder logs = new StringBuilder();
            int numberOfEntries;

            assert pathNames != null;
            for (String name : pathNames) {
                if (name.contains(tokenName[0])) {
                    userLogs.add(name);
                }
            }
            numberOfEntries = userLogs.size();
            if (numberOfEntries == 0) {
                specialCaseInt = 1;
                return null;
            }
            for (String log : userLogs) {
                logs.append(log).append("\r\n");
                File file = new File("battleLog/" + log);
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine()) {
                    logs.append(scanner.nextLine()).append("\r\n");
                }
                logs.append("\r\n");
                scanner.close();
            }
            return logs.toString();
        }else{
            specialCaseInt = 2;
            return null;
        }
    }

}
