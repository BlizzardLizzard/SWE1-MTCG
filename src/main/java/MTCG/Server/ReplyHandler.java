package MTCG.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReplyHandler {
    public String HTTPVersion = "HTTP/1.1";
    public String status = null;
    public String contentType = "application/json";
    public String body = "";
    public Socket socket;

    //takes care of all the possible replies that could happen
    public ReplyHandler(Socket socket){
        this.socket = socket;
    }


    public void userCreated(){
        status = "201 Created";
        body = "{\"Message\": \"An entry has been created\"}";
        printReply();
    }

    public void userAlreadyExists(){
        status = "403 Forbidden";
        body = "{\"Message\": \"User already exists\"}";
        printReply();
    }

    public void userLoggedIn(){
        status = "200 OK";
        body = "{\"Message\": \"User successfully logged in\"}";
        printReply();
    }

    public void userFailedLogin() {
        status = "401 Unauthorized";
        body = "{\"Message\": \"Wrong login info\"}";
        printReply();
    }

    public void userData(ResultSet userInfo) throws SQLException {
        status = "200 OK";
        body = "{\"Name\": \"" +userInfo.getString(1)+ "\",  \"Bio\": \"" + userInfo.getString(2) + "\", \"Image\": \"" + userInfo.getString(3) + "\"}";
        printReply();
    }

    public void userWrongToken() {
        status = "401 Unauthorized";
        body = "{\"Message\": \"Wrong token\"}";
        printReply();
    }

    public void setUserInfo(){
        status = "200 OK";
        body = "{\"Message\": \"Info has been set successfully\"}";
        printReply();
    }

    //optional feature shows stats but also win/loose/draw ratio
    public void getStats(ResultSet stats) throws SQLException {
        status = "200 OK";
        float winPercent = 0;
        float lossesPercent = 0;
        float drawPercent = 0;
        if(stats.getInt(5) != 0) {
            winPercent = (100 * stats.getInt(2)) / stats.getInt(5);
            lossesPercent = (100 * stats.getInt(3)) / stats.getInt(5);
            drawPercent = (100 * stats.getInt(4)) / stats.getInt(5);
        }
        body = "{\"Elo\": \"" + stats.getString(1)+ "\",\"NumberOfGamesPlayed\": \"" + stats.getInt(5) + "\", \"Wins\": \"" + stats.getInt(2) + "\", \"Losses\": \"" + stats.getInt(3) + "\", \"Draws\": \"" + stats.getInt(4) + "\", \"Win%/Loss%/Draws%\": \"" + winPercent + "/" + lossesPercent + "/" + drawPercent + "}";
        printReply();
    }

    public void getScore(String message) {
        status = "200 OK";
        body = message;
        printReply();
    }

    public void packageCreated(){
        status = "201 Created";
        body = "{\"Message\": \"A package has been created\"}";
        printReply();
    }

    public void boughtPackage(){
        status = "200 OK";
        body = "{\"Message\": \"User successfully bought a pack\"}";
        printReply();
    }

    public void userNoMoney() {
        status = "401 Unauthorized";
        body = "{\"Message\": \"User has no money\"}";
        printReply();
    }

    public void noMoreCards() {
        status = "401 Unauthorized";
        body = "{\"Message\": \"There are no packs left\"}";
        printReply();
    }

    public void tradeCreated(){
        status = "201 Created";
        body = "{\"Message\": \"A trade has been created\"}";
        printReply();
    }

    public void traded(){
        status = "200 OK";
        body = "{\"Message\": \"Trade successful\"}";
        printReply();
    }

    public void tradeNonExistent(){
        status = "404 Not Found";
        body = "{\"Message\": \"Trade could not be found\"}";
        printReply();
    }

    public void tradeWithYourself(){
        status = "403 Forbidden";
        body = "{\"Message\": \"Can not trade with yourself\"}";
        printReply();
    }

    public void nonExistingCard(){
        status = "404 Not Found";
        body = "{\"Message\": \"Card could not be found\"}";
        printReply();
    }

    public void requirementsError(){
        status = "403 Forbidden";
        body = "{\"Message\": \"Requirements are not met\"}";
        printReply();
    }

    public void tradeDeleted(){
        status = "200 OK";
        body = "{\"Message\": \"Trade successfully deleted\"}";
        printReply();
    }

    public void getTrades(String message){
        status = "200 OK";
        body = message;
        printReply();
    }

    public void getCards(String message){
        status = "200 OK";
        body = message;
        printReply();
    }

    //switches type of response (json or plain text) depending on request
    public void getDeck(String message, boolean plainText ){
        status = "200 OK";
        body = message;
        if(plainText){
            contentType = "text/plain";
        }
        printReply();
        contentType = "application/json";
    }

    public void deckCreated(){
        status = "201 Created";
        body = "{\"Message\": \"Your deck has been created\"}";
        printReply();
    }

    public void cardTokenError(){
        status = "401 Unauthorized";
        body = "{\"Message\": \"The cards are not the users cards\"}";
        printReply();
    }

    public void notEnoughCards(){
        status = "403 Forbidden";
        body = "{\"Message\": \"Not enough cards\"}";
        printReply();
    }

    public void player1SignedUp(){
        status = "200 OK";
        body = "{\"Message\": \"You successfully logged in to battle\"}";
        printReply();
    }

    public void getBattleLog(String message){
        status = "200 OK";
        body = message;
        contentType = "text/plain";
        printReply();
        contentType = "application/json";
    }

    public void samePlayer(){
        status = "403 Forbidden";
        body = "{\"Message\": \"You can not play against yourself\"}";
        printReply();
    }

    public void getLogs(String message){
        status = "200 OK";
        body = message;
        contentType = "text/plain";
        printReply();
        contentType = "application/json";
    }

    public void noLogEntries(){
        status = "200 OK";
        body = "{\"Message\": \"You have no log entries\"}";
        printReply();
    }

    public void noToken(){
        status = "401 Unauthorized";
        body = "{\"Message\": \"No token\"}";
        printReply();
    }

    public void generalErrorReply(){
        status = "400 Bad Request";
        body = "{\"Message\": \"Something went wrong\"}";
        printReply();
    }

    //prints the set reply
    public void printReply(){
        PrintWriter out = null;
        try {
            out = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert out != null;
        out.println(HTTPVersion + " " + status);
        out.println("Content-Type: " + contentType);
        out.println("");
        out.println(body);
        out.flush();
    }
}
