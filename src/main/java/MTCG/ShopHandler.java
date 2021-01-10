package MTCG;

import MTCG.JsonObjects.Card;
import MTCG.JsonObjects.Trade;
import MTCG.Server.ReplyHandler;
import MTCG.Server.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.Socket;
import java.sql.*;
import java.util.List;
import java.util.Locale;

public class ShopHandler {
    public String URI;
    public String request;
    public boolean requestHandeled;
    public int specialCaseInt;

    //takes care of all the incoming requests and directs them to the right response
    public ShopHandler(RequestContext requestContext, Socket socket) throws SQLException, ClassNotFoundException, JsonProcessingException {
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
        this.URI = requestContext.URI;
        this.request = requestContext.request;
        ReplyHandler replyHandler = new ReplyHandler(socket);
        if (URI.equals("/packages")) {
            requestHandeled = createPackage(requestContext, con);
            replyHandler.packageCreated();
        }
        if (URI.startsWith("/transactions/packages")) {
            requestHandeled = buyPackage(requestContext, con);
            if (requestHandeled) {
                replyHandler.boughtPackage();
            } else if (specialCaseInt == 1) {
                //user has no money
                replyHandler.userNoMoney();
            } else if (specialCaseInt == 2) {
                //there are no more cards available
                replyHandler.noMoreCards();
            }else{
                replyHandler.generalErrorReply();
            }
        }
        if(URI.equals("/tradings")){
            if(request.equals("POST")){
                requestHandeled = createTrade(requestContext, con);
                replyHandler.tradeCreated();
            }
            if(request.equals("GET")){
                String message = getTrades(requestContext, con);
                if(requestHandeled) {
                    replyHandler.getTrades(message);
                }else{
                    //token was not correct
                    replyHandler.userWrongToken();
                }
            }
        }
        if(URI.startsWith("/tradings/")) {
            if (request.equals("POST")) {
                requestHandeled = trade(requestContext, con);
                if (requestHandeled) {
                    replyHandler.traded();
                } else if (specialCaseInt == 1) {
                    //card does not meet requirements
                    replyHandler.requirementsError();
                } else if (specialCaseInt == 2) {
                    //card does not exist
                    replyHandler.nonExistingCard();
                } else if (specialCaseInt == 3) {
                    //user tries to trade with himself
                    replyHandler.tradeWithYourself();
                } else if (specialCaseInt == 4) {
                    //trade does not exist
                    replyHandler.tradeNonExistent();
                }else{
                    replyHandler.generalErrorReply();
                }
            }
            if(request.equals("DELETE")){
                requestHandeled = deleteTrade(requestContext, con);
                replyHandler.tradeDeleted();
            }
        }
    }

    public ShopHandler(){
    }

    //creates a package with the provided cards in the pack database
    public boolean createPackage(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<Card> listCard = mapper.readValue(requestContext.message, new TypeReference<List<Card>>() {
        });
        for (Card card : listCard) {
            PreparedStatement pst = con.prepareStatement("INSERT INTO pack(id, name, damage) VALUES(?,?,?) ");
            pst.setString(1, card.getId());
            pst.setString(2, card.getName());
            pst.setFloat(3, card.getDamage());
            pst.executeUpdate();
        }
        return true;
    }

    //player tries to buy a package
    public boolean buyPackage(RequestContext requestContext, Connection con) throws SQLException {
        int index = 1;
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] user = tokenSplit[1].split("-");

        int currentMoney = 0;
        PreparedStatement packAvailable = con.prepareStatement("SELECT count(*)  FROM pack");
        ResultSet pack = packAvailable.executeQuery();
        if (pack.next()) {
            int numberOfCards = pack.getInt(1);
            //checks if the pack database has a pack left
            if (numberOfCards != 0) {
                PreparedStatement money = con.prepareStatement("SELECT money FROM users WHERE username = ?");
                money.setString(1, user[0]);
                ResultSet moneySet = money.executeQuery();
                if (moneySet.next()) {
                    currentMoney = moneySet.getInt(1);
                }
                //checks if the player has any money left to buy a pack or the number of cards in pack is not 5
                if (currentMoney > 0 && numberOfCards >= 4) {
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
                            if (attributeLength == 1) {
                                element = "normal";
                                type = "monster";
                            }
                            if (attributeLength == 2) {
                                element = cardAtrributes[0].toLowerCase(Locale.ROOT);
                                if (cardAtrributes[1].equals("Spell")) {
                                    type = "spell";
                                    if (cardAtrributes[0].equals("Regular")) {
                                        element = "normal";
                                    }
                                } else {
                                    type = "monster";
                                }
                            }
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
                    return true;
                }
                //no more money
                specialCaseInt = 1;
                return false;
            }
            //no more cards
            specialCaseInt = 2;
            return false;
        }
        return false;
    }

    //creates a trade for the player with the requirements that the player set
    public boolean createTrade(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        String tradeId = jsonNode.get("Id").asText();
        String cardId = jsonNode.get("CardToTrade").asText();
        String type = jsonNode.get("Type").asText();
        int minimumDamage = jsonNode.get("MinimumDamage").asInt();
        String token = requestContext.authenticationToken(requestContext.requestString);
        PreparedStatement insertShop = con.prepareStatement("INSERT INTO shop(tradeid, cardid, token, typ, damage) VALUES (?,?,?,?,?)");
        insertShop.setString(1, tradeId);
        insertShop.setString(2, cardId);
        insertShop.setString(3, token);
        insertShop.setString(4, type);
        insertShop.setInt(5, minimumDamage);
        insertShop.executeUpdate();
        return true;
    }

    //player tries to trade
    public boolean trade(RequestContext requestContext, Connection con) throws SQLException {
        String[] tradeID = requestContext.URI.split("/");
        String token = requestContext.authenticationToken(requestContext.requestString);
        PreparedStatement getTokenFromTrade = con.prepareStatement("SELECT token, cardid, typ, damage FROM shop WHERE tradeid = ? AND traded = false");
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
            //checks if the player does not trade with himself
            if (!token.equals(dataBaseToken)) {
                String cardId = requestContext.message.replaceAll("^\"|\"$", "");
                float tradeDamage;
                String tradeType;
                PreparedStatement checkRequirements = con.prepareStatement("SELECT type, damage FROM stack WHERE player = ? AND id = ?");
                checkRequirements.setString(1, token);
                checkRequirements.setString(2, cardId);
                ResultSet requirements = checkRequirements.executeQuery();
                if (requirements.next()) {
                    tradeType = requirements.getString(1);
                    tradeDamage = requirements.getFloat(2);
                    //checks if wanted card type and minimum damage requirements are met
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
                        return true;
                    }
                    //card doesnt have right type or damage
                    specialCaseInt = 1;
                    return false;
                }
                //card that tries to be traded doesnt exist
                specialCaseInt = 2;
                return false;
            }
            //user tries to trade with himself
            specialCaseInt = 3;
            return false;
        }
        //trade does not exist
        specialCaseInt = 4;
        return false;
    }

    //get all the trades that are existing
    public String getTrades(RequestContext requestContext, Connection con) throws SQLException, JsonProcessingException {
        String token = requestContext.authenticationToken(requestContext.requestString);
        String[] tokenSplit = token.split(" ");
        String[] tokenName = tokenSplit[1].split("-");
        PreparedStatement userExists = con.prepareStatement("SELECT count(*) FROM users WHERE username = ?");
        userExists.setString(1,tokenName[0]);
        ResultSet playerExists = userExists.executeQuery();
        if(playerExists.next()){
            int rows = playerExists.getInt(1);
            //checks if player exists
            if(rows == 1){
                StringBuilder message = new StringBuilder();
                PreparedStatement trade = con.prepareStatement("SELECT * FROM shop");
                ResultSet trades = trade.executeQuery();
                ObjectMapper mapper = new ObjectMapper();
                message.append("[");
                while(trades.next()){
                    Trade tradeClass = new Trade(trades.getString(1), trades.getString(2), trades.getString(3), trades.getString(4),trades.getInt(5), trades.getBoolean(6));
                    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tradeClass);
                    message.append(json);
                    if(!trades.isLast()){
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

    //deletes the trade of the trader
    public boolean deleteTrade(RequestContext requestContext, Connection con) throws SQLException {
        String[] tradeId = requestContext.URI.split("/");
        String token = requestContext.authenticationToken(requestContext.requestString);

        PreparedStatement delete = con.prepareStatement("DELETE FROM shop WHERE token = ? AND tradeid = ?");
        delete.setString(1, token);
        delete.setString(2,tradeId[2]);
        delete.executeUpdate();
        return true;
    }
}
