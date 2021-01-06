package MTCG;

import MTCG.JsonObjects.CardStack;
import MTCG.Server.ReplyHandler;
import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.sql.*;

public class CardHandler {
    public String URI;
    public String request;
    public boolean requestHandeled;
    public int specialCaseInt;

    public CardHandler(RequestContext requestContext, Socket socket) throws SQLException, ClassNotFoundException, JsonProcessingException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
        this.URI = requestContext.URI;
        this.request = requestContext.request;
        ReplyHandler replyHandler = new ReplyHandler(socket);
        if(URI.equals("/cards")){
            String message = getCards(requestContext, con);
            if(requestHandeled){
                replyHandler.getCards(message);
            }else{
                replyHandler.userWrongToken();
            }
        }if(URI.startsWith("/deck")) {
            if (URI.equals("/deck")) {
                if (request.equals("GET")) {
                    String message = getDeck(requestContext, con, 0);
                    if (requestHandeled) {
                        replyHandler.getDeck(message);
                    } else {
                        replyHandler.userWrongToken();
                    }
                }
                if (request.equals("PUT")) {
                    requestHandeled = setDeck(requestContext, con);
                    if (requestHandeled) {
                        replyHandler.deckCreated();
                    } else if (specialCaseInt == 1) {
                        replyHandler.cardTokenError();
                    } else if (specialCaseInt == 2) {
                        replyHandler.notEnoughCards();
                    } else {
                        replyHandler.generalErrorReply();
                    }
                }
            }else{
                String message = getDeck(requestContext, con, 1);
                if(requestHandeled){
                    replyHandler.getDeck(message);
                }else{
                    replyHandler.userWrongToken();
                }
            }
        }
    }

    public String getCards(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        if(token != null){
            StringBuilder message = new StringBuilder();
            PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ?");
            cards.setString(1, token);
            ResultSet cardStack = cards.executeQuery();
            ObjectMapper mapper = new ObjectMapper();
            message.append("[");
            while(cardStack.next()){
                CardStack card = new CardStack(cardStack.getString(1), cardStack.getFloat(3), cardStack.getString(2));
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(card);
                message.append(json);
                if(!cardStack.isLast()){
                    message.append(",");
                }
            }
            message.append("]");
            requestHandeled = true;
            return message.toString();
        }
        requestHandeled = false;
        return null;
    }

    public String getDeck(RequestContext requestContext, Connection con, int textformat) throws SQLException, JsonProcessingException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        StringBuilder plainText = new StringBuilder();
        if(token != null){
            StringBuilder message = new StringBuilder();
            PreparedStatement cards = con.prepareStatement("SELECT id, name, damage FROM stack WHERE player = ? AND deck = true");
            cards.setString(1, token);
            ResultSet cardStack = cards.executeQuery();
            ObjectMapper mapper = new ObjectMapper();
            message.append("[");
            while(cardStack.next()){
                if(textformat == 1){
                    plainText.append("ID: ").append(cardStack.getString(1)).append(" Name: ").append(cardStack.getString(2)).append(" Damage: ").append(cardStack.getFloat(3)).append("\r\n");
                }
                CardStack card = new CardStack(cardStack.getString(1), cardStack.getFloat(3), cardStack.getString(2));
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(card);
                message.append(json);
                if (!cardStack.isLast()) {
                    message.append(",");
                }
            }
            message.append("]");
            requestHandeled = true;
            if(textformat == 1){
                return plainText.toString();
            }
            return message.toString();
        }
        requestHandeled = false;
        return null;
    }

    public boolean setDeck(RequestContext requestContext, Connection con) throws JsonProcessingException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        int numberOfCards = jsonNode.size();

        if (numberOfCards == 4) {
            for (int i = 0; i < 4; i++) {
                PreparedStatement checkCardToken = con.prepareStatement("SELECT count(*) AS number FROM stack WHERE id = ? AND player = ?");
                checkCardToken.setString(1, jsonNode.get(i).asText());
                checkCardToken.setString(2, requestContext.authenticationToken(requestContext.requestString));
                ResultSet cardHolder = checkCardToken.executeQuery();
                if (cardHolder.next()) {
                    int number = cardHolder.getInt(1);
                    if (number == 0) {
                        specialCaseInt = 1;
                        return false;
                    }
                }
            }

            for (int i = 0; i < 4; i++) {
                PreparedStatement searchForDeck = con.prepareStatement("SELECT id FROM stack WHERE deck = true AND player = ?");
                searchForDeck.setString(1, requestContext.authenticationToken(requestContext.requestString));
                ResultSet cardID = searchForDeck.executeQuery();
                String id;
                if (cardID.next()) {
                    id = cardID.getString(1);
                    PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = false WHERE id = ?");
                    updateDeck.setString(1, id);
                    updateDeck.executeUpdate();
                }
            }

            for (int i = 0; i < 4; i++) {
                String id = jsonNode.get(i).toString();
                //regex from https://stackoverflow.com/questions/2608665/how-can-i-trim-beginning-and-ending-double-quotes-from-a-string
                id = id.replaceAll("^\"|\"$", "");
                PreparedStatement cardExists = con.prepareStatement("SELECT count(*) FROM stack WHERE id = ? AND player = ?");
                cardExists.setString(1, id);
                cardExists.setString(2, requestContext.authenticationToken(requestContext.requestString));
                ResultSet cardCount = cardExists.executeQuery();
                int cardCountInt = 0;
                if (cardCount.next()) {
                    cardCountInt = cardCount.getInt(1);
                }
                if (cardCountInt == 1) {
                    PreparedStatement updateDeck = con.prepareStatement("UPDATE stack SET deck = true WHERE id = ?");
                    updateDeck.setString(1, id);
                    updateDeck.executeUpdate();
                }
            }
            return true;
        }
        specialCaseInt = 2;
        return false;
    }
}