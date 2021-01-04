package Server;

import MTCG.Card;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.List;
import java.util.Locale;

public class RequestHandler {
    public String request;
    public String status = " ";
    public String httpVersion = "HTTP/1.1";
    public String contentType = "text/plain";
    public String stringRequest = "";
    public int contentLength = 0;

    private boolean loggedIn = false;

    //filters the incoming requests to the right function needed
    public RequestHandler(String request, Socket socket, RequestContext requestContext) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Class.forName("org.postgresql.Driver");
            Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
            this.request = request;
            switch (request) {
                case "POST" -> PostRequest(requestContext, out, con);
                case "GET" -> GetRequest(requestContext, out, con);
                case "DELETE" -> DeleteRequest(requestContext, out, con);
                case "PUT" -> PutRequest(requestContext, out, con);
                default -> {
                    status = "400";
                    stringRequest = "Invalid request!";
                    contentLength = stringRequest.length();
                    PrintReply(out);
                    out.println(stringRequest);
                    out.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    //handles the POST requests
    public void PostRequest(RequestContext requestContext, PrintWriter out, Connection con) {
        try {

        if(requestContext.URI.equals("/users") || requestContext.URI.equals("/sessions")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestContext.message);
            String username = jsonNode.get("Username").asText();
            String password = jsonNode.get("Password").asText();

            if (requestContext.URI.equals("/users")) {
                PreparedStatement count = con.prepareStatement("SELECT count(*) AS total FROM users WHERE username = ?");
                count.setString(1, username);
                ResultSet resultSet = count.executeQuery();
                if (resultSet.next()) {
                    int rows = resultSet.getInt(1);
                    if (rows == 0) {
                        PreparedStatement pst = con.prepareStatement("INSERT INTO users(username, password) VALUES(?,?) ");
                        pst.setString(1, username);
                        pst.setString(2, password);
                        pst.executeUpdate();
                        status = "201";
                        stringRequest = "A new entry has been created";
                    }
                }
            }
            if (requestContext.URI.equals("/sessions")) {
                PreparedStatement data = con.prepareStatement("SELECT * FROM users WHERE username = ?");
                data.setString(1, username);
                ResultSet resultSet = data.executeQuery();
                if (resultSet.next()) {
                    if (password.equals(resultSet.getString(2))) {
                        status = "200";
                        stringRequest = "Successfully logged in";
                        loggedIn = true;
                    } else {
                        status = "401";
                        stringRequest = "Wrong login info";
                    }
                } else {
                    status = "400";
                    stringRequest = "Wrong structure";
                }
            }
        }
        if(requestContext.URI.equals("/packages")){
            ObjectMapper mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            List<Card> listCard = mapper.readValue(requestContext.message, new TypeReference<List<Card>>(){});

            for(Card card : listCard){
                PreparedStatement pst = con.prepareStatement("INSERT INTO pack(id, name, damage) VALUES(?,?,?) ");
                pst.setString(1, card.getId());
                pst.setString(2, card.getName());
                pst.setFloat(3, card.getDamage());
                pst.executeUpdate();
            }
            status = "201";
            stringRequest = "A new pack has been created";
        }
        if(requestContext.URI.equals("/transactions/packages")) {
            int index = 1;
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] user = tokenSplit[1].split("-");

            int currentMoney = 0;
            PreparedStatement packAvailable = con.prepareStatement("SELECT count(*)  FROM pack");
            ResultSet pack = packAvailable.executeQuery();
            if(pack.next()) {
                int numberOfCards = pack.getInt(1);
                PreparedStatement money = con.prepareStatement("SELECT money FROM users WHERE username = ?");
                money.setString(1, user[0]);
                ResultSet moneySet = money.executeQuery();
                if (moneySet.next()) {
                    currentMoney = moneySet.getInt(1);
                }
                if (currentMoney > 0 && numberOfCards > 4) {
                    currentMoney -= 5;
                    PreparedStatement updateMoney = con.prepareStatement("UPDATE users SET money = ? WHERE username = ?");
                    updateMoney.setInt(1, currentMoney);
                    updateMoney.setString(2, user[0]);
                    updateMoney.executeUpdate();

                    PreparedStatement number = con.prepareStatement("SELECT MIN(number) FROM pack");
                    ResultSet resultNumber = number.executeQuery();
                    if (resultNumber.next()) {
                        index = resultNumber.getInt(1);
                    }
                    for (int i = 1; i <= 5; i++) {
                        PreparedStatement data = con.prepareStatement("SELECT id, name, damage FROM pack WHERE number = ?");
                        data.setInt(1, index);
                        ResultSet resultSet = data.executeQuery();
                        index++;
                        if (resultSet.next()) {
                            String type = null;
                            String element = null;
                            String card = resultSet.getString(2);
                            //regex from https://stackoverflow.com/questions/7593969/regex-to-split-camelcase-or-titlecase-advanced
                            String[] cardAtrributes = card.split("(?<!^)(?=[A-Z])");
                            int attributeLength = cardAtrributes.length;
                            if(attributeLength == 1){
                                element = "normal";
                                type = "monster";
                            }
                            if(attributeLength == 2){
                                element = cardAtrributes[0].toLowerCase(Locale.ROOT);
                                if(cardAtrributes[1].equals("Spell")){
                                    type = "spell";
                                    if(cardAtrributes[0].equals("Regular")){
                                        element = "normal";
                                    }
                                }else{
                                    type = "monster";
                                }
                            }
                            System.out.println("ID: " + resultSet.getString(1) + " Name: " + resultSet.getString(2) + " Damage: " + resultSet.getFloat(3));
                            PreparedStatement insert = con.prepareStatement("INSERT INTO stack(id, name, damage, player, element, type) VALUES (?,?,?,?,?,?)");
                            insert.setString(1, resultSet.getString(1));
                            insert.setString(2, resultSet.getString(2));
                            insert.setFloat(3, resultSet.getFloat(3));
                            insert.setString(4, token);
                            insert.setString(5, element);
                            insert.setString(6, type);
                            insert.executeUpdate();

                            PreparedStatement delete = con.prepareStatement("DELETE FROM pack WHERE id = ?");
                            delete.setString(1, resultSet.getString(1));
                            delete.executeUpdate();
                            resultSet.next();
                        }
                    }
                    status = "201";
                    stringRequest = "A pack has been bought";
                }
            }
        }
        if(requestContext.URI.equals("/tradings")){
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestContext.message);
            String tradeId = jsonNode.get("Id").asText();
            String cardId = jsonNode.get("CardToTrade").asText();
            String type = jsonNode.get("Type").asText();
            int minimumDamage = jsonNode.get("MinimumDamage").asInt();
            String token = requestContext.authenticationToken(requestContext.requestString);
            //funktion um zu schauen ob user auch existiert! und ob karte überhaupt existiert
            PreparedStatement insertShop = con.prepareStatement("INSERT INTO shop(tradeid, cardid, token, typ, damage) VALUES (?,?,?,?,?)");
            insertShop.setString(1, tradeId);
            insertShop.setString(2, cardId);
            insertShop.setString(3, token);
            insertShop.setString(4, type);
            insertShop.setInt(5, minimumDamage);
            insertShop.executeUpdate();
        }
            if(requestContext.URI.startsWith("/tradings/")){
                String[] tradeID = requestContext.URI.split("/");
                String token = requestContext.authenticationToken(requestContext.requestString);
                PreparedStatement getTokenFromTrade = con.prepareStatement("SELECT token, cardid, typ, damage FROM shop WHERE tradeid = ?");
                getTokenFromTrade.setString(1,tradeID[2]);
                ResultSet dbToken = getTokenFromTrade.executeQuery();
                String dataBaseToken;
                String tradeCardId;
                String type;
                int minDamage;
                if(dbToken.next()) {
                    dataBaseToken = dbToken.getString(1);
                    tradeCardId = dbToken.getString(2);
                    type = dbToken.getString(3);
                    minDamage = dbToken.getInt(4);

                    if (!token.equals(dataBaseToken)) {
                        String cardId = requestContext.message.replaceAll("^\"|\"$", "");
                        float tradeDamage = 0;
                        String tradeType = null;
                        PreparedStatement checkRequirements = con.prepareStatement("SELECT type, damage FROM stack WHERE player = ? AND id = ?");
                        checkRequirements.setString(1, token);
                        checkRequirements.setString(2, cardId);
                        ResultSet requirements = checkRequirements.executeQuery();
                        if (requirements.next()) {
                            tradeType = requirements.getString(1);
                            tradeDamage = requirements.getFloat(2);
                        }
                        if (tradeType.equals(type) && tradeDamage >= minDamage) {
                            PreparedStatement tradeCard = con.prepareStatement("UPDATE shop SET traded = true WHERE tradeid = ?");
                            tradeCard.setString(1, tradeID[2]);
                            tradeCard.executeUpdate();


                            PreparedStatement idSwap1 = con.prepareStatement("UPDATE stack SET player = ? WHERE id = ?");
                            idSwap1.setString(1, token);
                            idSwap1.setString(2, tradeCardId);
                            idSwap1.executeUpdate();

                            PreparedStatement idSwap2 = con.prepareStatement("UPDATE stack SET player = ? WHERE id = ?");
                            idSwap2.setString(1, dataBaseToken);
                            idSwap2.setString(2, cardId);
                            idSwap2.executeUpdate();
                        }
                    }
                }
            }
                contentLength = stringRequest.length();
                PrintReply(out);
                out.println(stringRequest);
                out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
    }

    //handles the GET requests
    public void GetRequest(RequestContext requestContext, PrintWriter out, Connection con) throws SQLException {

        if(requestContext.URI.equals("/cards")){
            String token = requestContext.authenticationToken(requestContext.requestString);
            if(token != null){
                PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ?");
                cards.setString(1, token);
                ResultSet cardStack = cards.executeQuery();
                while(cardStack.next()){
                    System.out.println("ID: " + cardStack.getString(1) + " Name: " + cardStack.getString(2) + " Damage: " + cardStack.getFloat(3));
                }
            }
        }
        if(requestContext.URI.equals("/deck")){
            String token = requestContext.authenticationToken(requestContext.requestString);
            if(token != null){
                PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ? AND deck = true");
                cards.setString(1, token);
                ResultSet cardStack = cards.executeQuery();
                System.out.println(token);
                while(cardStack.next()){
                    System.out.println("ID: " + cardStack.getString(1) + " Name: " + cardStack.getString(2) + " Damage: " + cardStack.getFloat(3));
                }
            }
        }
        if(requestContext.URI.startsWith("/users/")){
            String[] user = requestContext.URI.split("/");
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");
            if(user[2].equals(tokenName[0])){
                PreparedStatement userBio = con.prepareStatement("SELECT name, bio, image FROM users WHERE username = ?");
                userBio.setString(1,user[2]);
                ResultSet userInfo = userBio.executeQuery();
                if(userInfo.next()){
                    System.out.println("Name: " + userInfo.getString(1));
                    System.out.println("Bio: " + userInfo.getString(2));
                    System.out.println("Image: " + userInfo.getString(3));
                }
            }
        }
        if(requestContext.URI.equals("/stats")){
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");
            PreparedStatement stats = con.prepareStatement("SELECT elo, wins, losses, draws, gamesplayed FROM users WHERE username = ?");
            stats.setString(1,tokenName[0]);
            ResultSet resultStats = stats.executeQuery();
            if(resultStats.next()){
                System.out.println("Elo: " + resultStats.getString(1));
                System.out.println("Number of games played: " + resultStats.getInt(5));
                System.out.println(resultStats.getInt(2) + "/" + resultStats.getInt(3) + "/" + resultStats.getInt(4));
            }
        }
        if(requestContext.URI.equals("/score")){
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");
            PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
            userExists.setString(1,tokenName[0]);
            ResultSet playerExists = userExists.executeQuery();
            if(playerExists.next()){
                int rows = playerExists.getInt(1);
                if(rows == 1){
                    PreparedStatement score = con.prepareStatement("SELECT username, elo FROM users ORDER BY elo DESC");
                    ResultSet scoreboard = score.executeQuery();
                    while(scoreboard.next()){
                        System.out.println("Username: " + scoreboard.getString(1) + " Elo: " + scoreboard.getInt(2));
                    }
                }
            }
        }
        if(requestContext.URI.equals("/tradings")){
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");
            PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
            userExists.setString(1,tokenName[0]);
            ResultSet playerExists = userExists.executeQuery();
            if(playerExists.next()){
                int rows = playerExists.getInt(1);
                if(rows == 1){
                    PreparedStatement score = con.prepareStatement("SELECT * FROM shop WHERE traded = false");
                    ResultSet scoreboard = score.executeQuery();
                    while(scoreboard.next()){
                        System.out.println("Shop");
                    }
                }
            }
        }
    }

    //handles the DELETE requests
    public void DeleteRequest(RequestContext requestContext, PrintWriter out, Connection con) throws SQLException {
        if(requestContext.URI.startsWith("/tradings")){
            String[] tradeId = requestContext.URI.split("/");
            String token = requestContext.authenticationToken(requestContext.requestString);

            PreparedStatement delete = con.prepareStatement("DELETE FROM shop WHERE token = ? AND tradeid = ?");
            delete.setString(1, token);
            delete.setString(2,tradeId[2]);
            delete.executeUpdate();
        }
        /*String path = requestContext.getURI();
        String[] pathSplit = path.split("/");
        int numberOfStrings = pathSplit.length;
        //checks if an ID has been give to delete is existing with if(deleteFile.delete()) if so its deleted else the file did not exist
            if(numberOfStrings > 2){
                File deleteFile = new File("messages/" + pathSplit[2] + ".txt");
               //file kann noch offen sein!
                if(deleteFile.delete()){
                    status = "200";
                    stringRequest = pathSplit[2] + ".txt has been successfully deleted!";
                }else{
                    status = "404";
                    stringRequest = "File could not be found!";
                }
            }else{
                status = "400";
                stringRequest = "Please enter an ID!";
            }*/
        contentLength = stringRequest.length();
        PrintReply(out);
        out.println(stringRequest);
        out.flush();
    }

    //handles the PUT requests
    public void PutRequest(RequestContext requestContext, PrintWriter out, Connection con) throws IOException, SQLException {
        if (requestContext.URI.equals("/deck")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestContext.message);
            int numberOfCards = jsonNode.size();

            if (numberOfCards == 4) {
                for (int i = 0; i < 4; i++) {
                    PreparedStatement searchForDeck = con.prepareStatement("SELECT id FROM stack WHERE deck = true AND player = ?");
                    searchForDeck.setString(1, requestContext.authenticationToken(requestContext.requestString));
                    ResultSet cardID = searchForDeck.executeQuery();
                    String id = null;
                    if (cardID.next()) {
                        id = cardID.getString(1);
                    }
                    PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = ? WHERE id = ?");
                    updateDeck.setBoolean(1, false);
                    updateDeck.setString(2, id);
                    updateDeck.executeUpdate();
                }

                for (int i = 0; i < 4; i++) {
                    String id = jsonNode.get(i).toString();
                    //regex from https://stackoverflow.com/questions/2608665/how-can-i-trim-beginning-and-ending-double-quotes-from-a-string
                    id = id.replaceAll("^\"|\"$", "");
                    System.out.println(id);
                    PreparedStatement cardExists = con.prepareStatement("SELECT count(*) FROM stack WHERE id = ? AND player = ?");
                    cardExists.setString(1, id);
                    cardExists.setString(2, requestContext.authenticationToken(requestContext.requestString));
                    ResultSet cardCount = cardExists.executeQuery();
                    int cardCountInt = 0;
                    if (cardCount.next()) {
                        cardCountInt = cardCount.getInt(1);
                    }
                    if (cardCountInt == 1) {
                        PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = ? WHERE id = ?");
                        updateDeck.setBoolean(1, true);
                        updateDeck.setString(2, id);
                        updateDeck.executeUpdate();
                    }
                }
            }
        }
        if(requestContext.URI.startsWith("/users/")){
            ObjectMapper mapper = new ObjectMapper();
            String[] user = requestContext.URI.split("/");
            String token = requestContext.authenticationToken(requestContext.requestString);
            String[] tokenSplit = token.split(" ");
            String[] tokenName = tokenSplit[1].split("-");
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
            }
        }
    }

    //responsible for printing the header of the reply
    public void PrintReply(PrintWriter out){
        out.println(httpVersion + " " + status);
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + contentLength);
        out.println("");
    }
}


