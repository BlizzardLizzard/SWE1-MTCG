package MTCG;

import MTCG.JsonObjects.BattleCards;
import MTCG.Server.ReplyHandler;
import MTCG.Server.RequestContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class BattleHandler {

    public String player1Token = "";
    public String player2Token = "";
    public String player1Name;
    public String player2Name;
    public float damagePlayer1;
    public float damagePlayer2;
    public String message;

    public BattleHandler(RequestContext requestContext, Socket socket) throws ClassNotFoundException, SQLException, IOException {
        int numberOfPlayers = -1;
        ReplyHandler replyHandler = new ReplyHandler(socket);
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");

        //check number of cards of player
        int cards = 0;
        PreparedStatement cardsInDeck = con.prepareStatement("SELECT count(*) FROM stack WHERE deck = true AND player = ?");
        cardsInDeck.setString(1, requestContext.authenticationToken(requestContext.requestString));
        ResultSet numberOfCards = cardsInDeck.executeQuery();
        if(numberOfCards.next()){
            cards = numberOfCards.getInt(1);
        }

        //create game if no game exists in the database
        PreparedStatement gameInDB = con.prepareStatement("SELECT count(*) FROM battle WHERE id = 1");
        ResultSet numberDb = gameInDB.executeQuery();
        int game = 0;
        if(numberDb.next()){
            game = numberDb.getInt(1);
        }
        if(game == 0){
            PreparedStatement startBattle = con.prepareStatement("INSERT INTO battle (id, players, player1, player2) VALUES (1, 0, NULL, NULL)");
            startBattle.executeUpdate();
        }

        if(cards == 4) {
            PreparedStatement players = con.prepareStatement("SELECT players FROM battle");
            ResultSet number = players.executeQuery();
            if (number.next()) {
                numberOfPlayers = number.getInt(1);
            }

            //register first player if no one has registered to battle yet
            if (numberOfPlayers == 0) {
                PreparedStatement player1 = con.prepareStatement("UPDATE battle SET player1 = ?, players = 1 WHERE id = 1");
                player1.setString(1, requestContext.authenticationToken(requestContext.requestString));
                player1.executeUpdate();
                replyHandler.player1SignedUp();
            }

            //register second player and start battle
            if (numberOfPlayers == 1) {
                PreparedStatement player2 = con.prepareStatement("UPDATE battle SET player2 = ? WHERE id = 1");
                player2.setString(1, requestContext.authenticationToken(requestContext.requestString));
                player2.executeUpdate();
                player2Token = requestContext.authenticationToken(requestContext.requestString);

                PreparedStatement player1 = con.prepareStatement("SELECT player1 FROM battle");
                ResultSet player = player1.executeQuery();
                if (player.next()) {
                    player1Token = player.getString(1);
                }
                if (!player1Token.equals(player2Token)) {
                    startBattle(con);
                    replyHandler.getBattleLog(message);
                    createBattleLog();
                }else{
                    //the player tried to sign in 2 times
                    replyHandler.samePlayer();
                }
            }
        }else{
            //the players does not have 4 cards in his deck
            replyHandler.notEnoughCards();
        }
    }

    public void startBattle(Connection con) throws SQLException {
        int result;
        //get name of player 1
        String[] tokenSplitPlayer1 = player1Token.split(" ");
        String[] tokenName1 = tokenSplitPlayer1[1].split("-");
        player1Name = tokenName1[0];

        //get name of player 2
        String[] tokenSplitPlayer2 = player2Token.split(" ");
        String[] tokenName2 = tokenSplitPlayer2[1].split("-");
        player2Name = tokenName2[0];

        ArrayList<BattleCards> player1 = new ArrayList<>();
        ArrayList<BattleCards> player2 = new ArrayList<>();

        //creating deck for player1
        PreparedStatement player1Card = con.prepareStatement("SELECT name, damage, element, type FROM stack WHERE player = ? AND deck = true");
        player1Card.setString(1,player1Token);
        ResultSet player1Cards = player1Card.executeQuery();
        while (player1Cards.next()) {
            player1.add(new BattleCards(player1Cards.getString(1), player1Cards.getFloat(2), player1Cards.getString(3), player1Cards.getString(4)));
        }

        //creating deck for player2
        PreparedStatement player2Card = con.prepareStatement("SELECT name, damage, element, type FROM stack WHERE player = ? AND deck = true");
        player2Card.setString(1,player2Token);
        ResultSet player2Cards = player2Card.executeQuery();
        while (player2Cards.next()) {
            player2.add(new BattleCards(player2Cards.getString(1), player2Cards.getFloat(2), player2Cards.getString(3), player2Cards.getString(4)));
        }
        //start battle
        result = battleLogic(player1, player2);
        setNewStats(result, player1Name, player2Name, con);
    }

    public int battleLogic(ArrayList<BattleCards> player1, ArrayList<BattleCards> player2) {
        int numberOfGames = 1;
        StringBuilder battle = new StringBuilder();
        //checks if 100 games have been played or one of the players has no cards left
        while (player1.size() != 0 && player2.size() != 0 && numberOfGames < 101) {
            boolean specialEvent = false;

            //random card chosen for player1 and player2
            Random rand1 = new Random();
            Random rand2 = new Random();
            int cardPlayer1 = rand1.nextInt(player1.size());
            int cardPlayer2 = rand2.nextInt(player2.size());

            //data of player1
            String cardNamePlayer1 = player1.get(cardPlayer1).getName();
            String typePlayer1 = player1.get(cardPlayer1).getType();
            String elementPlayer1 = player1.get(cardPlayer1).getElement();
            damagePlayer1 = player1.get(cardPlayer1).getDamage();

            //data of player2
            String cardNamePlayer2 = player2.get(cardPlayer2).getName();
            String typePlayer2 = player2.get(cardPlayer2).getType();
            String elementPlayer2 = player2.get(cardPlayer2).getElement();
            damagePlayer2 = player2.get(cardPlayer2).getDamage();

            //monster vs monster
            if (typePlayer1.equals(typePlayer2) && typePlayer1.equals("monster")) {
                //goblin vs dragon
                if ((cardNamePlayer1.contains("Goblin") && cardNamePlayer2.contains("Dragon")) || (cardNamePlayer1.contains("Dragon") && cardNamePlayer2.contains("Goblin"))) {
                    if (cardNamePlayer1.contains("Dragon")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The goblin is too afraid to attack").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(cardNamePlayer1).append(" defeats ").append(player2Name).append(" ").append(cardNamePlayer2).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The goblin is too afraid to attack").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(cardNamePlayer2).append(" defeats ").append(player1Name).append(" ").append(cardNamePlayer1).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //wizard vs ork
                if ((cardNamePlayer1.contains("Wizard") && cardNamePlayer2.contains("Ork")) || (cardNamePlayer1.contains("Ork") && cardNamePlayer2.contains("Wizard"))) {
                    if (cardNamePlayer1.contains("Wizard")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The wizard controls the ork").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(cardNamePlayer1).append(" defeats ").append(player2Name).append(" ").append(cardNamePlayer2).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The wizard controls the ork").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(cardNamePlayer2).append(" defeats ").append(player1Name).append(" ").append(cardNamePlayer1).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //fireElf vs dragon
                if ((cardNamePlayer1.contains("Elf") && cardNamePlayer2.contains("Dragon")) || (cardNamePlayer1.contains("Dragon") && cardNamePlayer2.contains("Elf"))) {
                    if (cardNamePlayer1.contains("Elf")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The elf evaded the attack").append("\r\n");
                        battle.append("=> ").append(player1Name).append(" ").append(cardNamePlayer1).append(" defeats ").append(player2Name).append(" ").append(cardNamePlayer2).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The elf evaded the attack").append("\r\n");
                        battle.append("=> ").append(player2Name).append(" ").append(cardNamePlayer2).append(" defeats ").append(player1Name).append(" ").append(cardNamePlayer1).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //monster vs monster damage comparison
                if (!specialEvent) {
                    if (damagePlayer1 == damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> Draw").append("\r\n");
                    } else if (damagePlayer1 > damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> ").append(player1Name).append(" ").append(cardNamePlayer1).append(" defeats ").append(player2Name).append(" ").append(cardNamePlayer2).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else if (damagePlayer1 < damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("=> ").append(player2Name).append(" ").append(cardNamePlayer2).append(" defeats ").append(player1Name).append(" ").append(cardNamePlayer1).append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                }
            }

            //spell vs spell
            if (typePlayer1.equals(typePlayer2) && typePlayer1.equals("spell")) {
                printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                effectiveness(elementPlayer1, elementPlayer2, player1, player2, cardPlayer1, cardPlayer2);
                printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2, battle);
            }

            //spell vs monster
            if (!typePlayer1.equals(typePlayer2)) {
                //knight vs waterSpell
                if ((cardNamePlayer1.equals("Knight") && cardNamePlayer2.equals("WaterSpell")) || (cardNamePlayer1.equals("WaterSpell") && cardNamePlayer2.equals("Knight"))) {
                    if (cardNamePlayer1.equals("Knight")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The knight drowned").append("\r\n");
                        battle.append("-> (").append(player2Name).append(") ").append(cardNamePlayer2).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The knight drowned").append("\r\n");
                        battle.append("-> (").append(player1Name).append(") ").append(cardNamePlayer1).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    }
                    specialEvent = true;
                }

                //kraken vs spell
                if ((cardNamePlayer1.equals("Kraken") && typePlayer2.equals("spell")) || (typePlayer1.equals("spell") && cardNamePlayer2.equals("Kraken"))) {
                    if (cardNamePlayer1.equals("Kraken")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The kraken is immune to spells").append("\r\n");
                        battle.append("-> (").append(player1Name).append(") ").append(cardNamePlayer1).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        battle.append("The kraken is immune to spells").append("\r\n");
                        battle.append("-> (").append(player2Name).append(") ").append(cardNamePlayer2).append(" wins").append("\r\n");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //same element vs same element
                if(!specialEvent) {
                    if (elementPlayer1.equals(elementPlayer2)) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2, battle);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2, battle);
                        effectiveness(elementPlayer1, elementPlayer2, player1, player2, cardPlayer1, cardPlayer2);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2, battle);
                    }
                }
            }
            //overview of current standings between the players
            battle.append("Number of cards Player 1: ").append(player1.size()).append("\r\n");
            battle.append("Number of cards Player 2: ").append(player2.size()).append("\r\n");
            battle.append("Number of round: ").append(numberOfGames).append("\r\n").append("\r\n");

            numberOfGames ++;
        }
        //requirement for a draw
        if(numberOfGames >= 100){
            battle.append("The game has been drawn");
            message = battle.toString();
            return 0;
        }else {
            //if player 2 has 0 cards in his deck player wins and the other way around
            if (player2.size() == 0) {
                battle.append("Player 1 has won (").append(player1Name).append(")");
                message = battle.toString();
                return 1;
            } else {
                battle.append("Player 2 has won (").append(player2Name).append(")");
                message = battle.toString();
                return 2;
            }
        }
    }

    //prints out the damage and name of the card and who it belongs to
    public void printBattle(String cardNamePlayer1, String cardNamePlayer2, float damagePlayer1, float damagePlayer2, StringBuilder battle) {
            battle.append("Player 1 (").append(player1Name).append("): ").append(cardNamePlayer1).append(" (").append(damagePlayer1).append(" Damage) vs Player 2 (").append(player2Name).append("): ").append(cardNamePlayer2).append(" (").append(damagePlayer2).append(" Damage)").append("\r\n");
    }

    //if a spell and a monster or a spell and a spell fight against each other then the reply string is different to a monster vs monster fight
    public void printSpellAndMixedResults(float damagePlayer1, float damagePlayer2, String cardNamePlayer1, String cardNamePlayer2, ArrayList<BattleCards> player1, ArrayList<BattleCards> player2, int cardPlayer1, int cardPlayer2, StringBuilder battle){
        if(damagePlayer1 == damagePlayer2){
            battle.append("=> Damage: ").append(cardNamePlayer1).append(": ").append(damagePlayer1).append(" vs ").append(cardNamePlayer2).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> Draw").append("\r\n");
        }
        if(damagePlayer1 > damagePlayer2){
            battle.append("=> Damage: ").append(cardNamePlayer1).append(": ").append(damagePlayer1).append(" vs ").append(cardNamePlayer2).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> (").append(player1Name).append(") ").append(cardNamePlayer1).append(" wins").append("\r\n");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
        }if(damagePlayer1 < damagePlayer2){
            battle.append("=> Damage: ").append(cardNamePlayer1).append(": ").append(damagePlayer1).append(" vs ").append(cardNamePlayer2).append(": ").append(damagePlayer2).append("\r\n");
            battle.append("-> (").append(player2Name).append(") ").append(cardNamePlayer2).append(" wins").append("\r\n");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
        }
    }

    //effectiveness of the mixed fights or spell fights is calculated by their element type water->fire, fire->normal, normal, water
    public void effectiveness(String elementPlayer1, String elementPlayer2, ArrayList<BattleCards> player1, ArrayList<BattleCards> player2, int cardPlayer1, int cardPlayer2){
        //fire vs water
        if ((elementPlayer1.equals("fire") && elementPlayer2.equals("water")) || (elementPlayer1.equals("water") && elementPlayer2.equals("fire"))) {
            //calculate damage
            if (elementPlayer1.equals("fire")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }

        //normal vs fire
        if ((elementPlayer1.equals("fire") && elementPlayer2.equals("normal")) || (elementPlayer1.equals("normal") && elementPlayer2.equals("fire"))) {
            //calculate damage
            if (elementPlayer1.equals("normal")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }

        //water vs normal
        if ((elementPlayer1.equals("normal") && elementPlayer2.equals("water")) || (elementPlayer1.equals("water") && elementPlayer2.equals("normal"))) {
            //calculate damage
            if (elementPlayer1.equals("water")) {
                damagePlayer1 = player1.get(cardPlayer1).getDamage()/2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()*2;
            }else{
                damagePlayer1 = player1.get(cardPlayer1).getDamage()*2;
                damagePlayer2 = player2.get(cardPlayer2).getDamage()/2;
            }
        }
    }

    //removes a card from the player that lost and adds it to the winners deck
    public void adjustDeck(ArrayList<BattleCards> player1, ArrayList<BattleCards> player2, int cardPlayer1, int cardPlayer2, int player){
        if(player == 1){
            player1.add(player2.get(cardPlayer2));
            player2.remove(cardPlayer2);
        }else if(player == 2){
            player2.add(player1.get(cardPlayer1));
            player1.remove(cardPlayer1);
        }
    }

    //updates the stats of players after the battle has ended (-5 elo for a loss and +3 elo for a win)
    public void setNewStats(int result, String player1Name, String player2Name, Connection con) throws SQLException {
        int eloPlayer1 = 0;
        int winsPlayer1 = 0;
        int drawsPlayer1 = 0;
        int lossesPlayer1 = 0;
        int gamesPlayer1 = 0;

        int eloPlayer2 = 0;
        int winsPlayer2 = 0;
        int drawsPlayer2 = 0;
        int lossesPlayer2 = 0;
        int gamesPlayer2 = 0;

        //get data from player1
        PreparedStatement player1 = con.prepareStatement("SELECT elo, wins, draws, losses, gamesplayed FROM users WHERE username = ?");
        player1.setString(1, player1Name);
        ResultSet statsUpdate1 = player1.executeQuery();
        if(statsUpdate1.next()){
            eloPlayer1 = statsUpdate1.getInt(1);
            winsPlayer1 = statsUpdate1.getInt(2);
            drawsPlayer1 = statsUpdate1.getInt(3);
            lossesPlayer1 = statsUpdate1.getInt(4);
            gamesPlayer1 = statsUpdate1.getInt(5);
        }

        //get data from player2
        PreparedStatement player2 = con.prepareStatement("SELECT elo, wins, draws, losses, gamesplayed FROM users WHERE username = ?");
        player2.setString(1, player2Name);
        ResultSet statsUpdate2 = player2.executeQuery();
        if(statsUpdate2.next()){
            eloPlayer2 = statsUpdate2.getInt(1);
            winsPlayer2 = statsUpdate2.getInt(2);
            drawsPlayer2 = statsUpdate2.getInt(3);
            lossesPlayer2 = statsUpdate2.getInt(4);
            gamesPlayer2 = statsUpdate2.getInt(5);
        }

        gamesPlayer1 += 1;
        gamesPlayer2 += 1;

        //draw
        if(result == 0){
            drawsPlayer1 += 1;
            drawsPlayer2 += 1;

            //update player1
            PreparedStatement drawPlayer1 = con.prepareStatement("UPDATE users SET draws = ?, gamesplayed = ? WHERE username = ?");
            drawPlayer1.setInt(1, drawsPlayer1);
            drawPlayer1.setInt(2, gamesPlayer1);
            drawPlayer1.setString(3, player1Name);
            drawPlayer1.executeUpdate();

            //update player2
            PreparedStatement drawPlayer2 = con.prepareStatement("UPDATE users SET draws = ?, gamesplayed = ? WHERE username = ?");
            drawPlayer2.setInt(1, drawsPlayer2);
            drawPlayer2.setInt(2, gamesPlayer2);
            drawPlayer2.setString(3, player2Name);
            drawPlayer2.executeUpdate();
        }
        //player1 won
        if(result == 1){
            eloPlayer1 += 3;
            eloPlayer2 -= 5;
            winsPlayer1 += 1;
            lossesPlayer2 += 1;

            //update player1
            PreparedStatement winPlayer1 = con.prepareStatement("UPDATE users SET wins = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            winPlayer1.setInt(1, winsPlayer1);
            winPlayer1.setInt(2, gamesPlayer1);
            winPlayer1.setInt(3, eloPlayer1);
            winPlayer1.setString(4, player1Name);
            winPlayer1.executeUpdate();

            //update player2
            PreparedStatement losePlayer2 = con.prepareStatement("UPDATE users SET losses = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            losePlayer2.setInt(1, lossesPlayer2);
            losePlayer2.setInt(2, gamesPlayer2);
            losePlayer2.setInt(3, eloPlayer2);
            losePlayer2.setString(4, player2Name);
            losePlayer2.executeUpdate();
        }
        //player2 won
        if(result == 2){
            eloPlayer2 += 3;
            eloPlayer1 -= 5;
            winsPlayer2 += 1;
            lossesPlayer1 += 1;

            //update player1
            PreparedStatement losePlayer1 = con.prepareStatement("UPDATE users SET losses = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            losePlayer1.setInt(1, lossesPlayer1);
            losePlayer1.setInt(2, gamesPlayer1);
            losePlayer1.setInt(3, eloPlayer1);
            losePlayer1.setString(4, player1Name);
            losePlayer1.executeUpdate();

            //update player2
            PreparedStatement winPlayer2 = con.prepareStatement("UPDATE users SET wins = ?, gamesplayed = ?, elo = ? WHERE username = ?");
            winPlayer2.setInt(1, winsPlayer2);
            winPlayer2.setInt(2, gamesPlayer2);
            winPlayer2.setInt(3, eloPlayer2);
            winPlayer2.setString(4, player2Name);
            winPlayer2.executeUpdate();
        }
        PreparedStatement resetBattle = con.prepareStatement("UPDATE battle SET players = 0, player1 = NULL, player2 = NULL WHERE id = 1");
        resetBattle.executeUpdate();
    }

    //saves the battle log into a folder for the possibility to later let each player look at their fought battles (unique feature)
    public void createBattleLog() throws IOException {
        int numberOfEntriesInDirectory = Objects.requireNonNull(new File("battleLog/").listFiles()).length;
        numberOfEntriesInDirectory += 1;
        File createLog = new File("battleLog/" + numberOfEntriesInDirectory + "-" + player1Name + " vs " + player2Name);

        if(!createLog.exists()){
            FileWriter writer = new FileWriter(createLog);
            writer.write(message);
            writer.close();
        }
    }
}
