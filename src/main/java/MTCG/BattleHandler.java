package MTCG;

import MTCG.JsonObjects.BattleCards;
import MTCG.Server.ReplyHandler;
import MTCG.Server.RequestContext;

import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Random;

public class BattleHandler {

    public String player1Token;
    public String player2Token;
    public String player1Name;
    public String player2Name;
    public float damagePlayer1;
    public float damagePlayer2;

    public BattleHandler(RequestContext requestContext, Socket socket) throws ClassNotFoundException, SQLException {
        int numberOfPlayers = -1;
        ReplyHandler replyHandler = new ReplyHandler(socket);
        Class.forName("org.postgresql.Driver");
        Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/MTCG", "postgres", "passwort");
        PreparedStatement players = con.prepareStatement("SELECT players FROM battle");
        ResultSet number = players.executeQuery();
        if(number.next()){
            numberOfPlayers = number.getInt(1);
        }
        if(numberOfPlayers == 0){
            PreparedStatement player1 = con.prepareStatement("UPDATE battle SET player1 = ?, players = 1 WHERE id = 1");
            player1.setString(1,requestContext.authenticationToken(requestContext.requestString));
            player1.executeUpdate();
            replyHandler.player1SignedUp();
        }
        if(numberOfPlayers == 1){
            PreparedStatement player2 = con.prepareStatement("UPDATE battle SET player2 = ? WHERE id = 1");
            player2.setString(1,requestContext.authenticationToken(requestContext.requestString));
            player2.executeUpdate();
            player2Token = requestContext.authenticationToken(requestContext.requestString);

            PreparedStatement player1 = con.prepareStatement("SELECT player1 FROM battle");
            ResultSet player = player1.executeQuery();
            if(player.next()){
                player1Token = player.getString(1);
            }

            startBattle(con);
        }
    }

    public void startBattle(Connection con) throws SQLException {
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
        battleLogic(player1, player2);
    }

    public void battleLogic(ArrayList<BattleCards> player1, ArrayList<BattleCards> player2) {
        int numberOfGames = 0;
        while (player1.size() != 0 && player2.size() != 0 && numberOfGames < 101) {
            boolean specialEvent = false;

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
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The goblin is too afraid to attack");
                        System.out.println("=> " + player1Name + " " + cardNamePlayer1 + " defeats " + player2Name + " " + cardNamePlayer2);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The goblin is too afraid to attack");
                        System.out.println("=> " + player2Name + " " + cardNamePlayer2 + " defeats " + player1Name + " " + cardNamePlayer1);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //wizard vs ork
                if ((cardNamePlayer1.contains("Wizard") && cardNamePlayer2.contains("Ork")) || (cardNamePlayer1.contains("Ork") && cardNamePlayer2.contains("Wizard"))) {
                    if (cardNamePlayer1.contains("Wizard")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The wizard controls the ork");
                        System.out.println("=> " + player1Name + " " + cardNamePlayer1 + " defeats " + player2Name + " " + cardNamePlayer2);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The wizard controls the ork");
                        System.out.println("=> " + player2Name + " " + cardNamePlayer2 + " defeats " + player1Name + " " + cardNamePlayer1);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //fireElf vs dragon
                if ((cardNamePlayer1.contains("Elf") && cardNamePlayer2.contains("Dragon")) || (cardNamePlayer1.contains("Dragon") && cardNamePlayer2.contains("Elf"))) {
                    if (cardNamePlayer1.contains("Elf")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The elf evaded the attack");
                        System.out.println("=> " + player1Name + " " + cardNamePlayer1 + " defeats " + player2Name + " " + cardNamePlayer2);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The elf evaded the attack");
                        System.out.println("=> " + player2Name + " " + cardNamePlayer2 + " defeats " + player1Name + " " + cardNamePlayer1);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //monster vs monster damage comparison
                if (!specialEvent) {
                    if (damagePlayer1 == damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("=> Draw");
                    } else if (damagePlayer1 > damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("=> " + player1Name + " " + cardNamePlayer1 + " defeats " + player2Name + " " + cardNamePlayer2);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else if (damagePlayer1 < damagePlayer2) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("=> " + player2Name + " " + cardNamePlayer2 + " defeats " + player1Name + " " + cardNamePlayer1);
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                }
            }

            //spell vs spell
            if (typePlayer1.equals(typePlayer2) && typePlayer1.equals("spell")) {
                printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                effectiveness(elementPlayer1, elementPlayer2, player1, player2, cardPlayer1, cardPlayer2);
                printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2);
            }

            //spell vs monster
            if (!typePlayer1.equals(typePlayer2)) {
                //knight vs waterSpell
                if ((cardNamePlayer1.equals("Knight") && cardNamePlayer2.equals("WaterSpell")) || (cardNamePlayer1.equals("WaterSpell") && cardNamePlayer2.equals("Knight"))) {
                    if (cardNamePlayer1.equals("Knight")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The knight drowned");
                        System.out.println("-> (" + player2Name + ") " + cardNamePlayer2 + " wins");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The knight drowned");
                        System.out.println("-> (" + player1Name + ") " + cardNamePlayer1 + " wins");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    }
                    specialEvent = true;
                }

                //kraken vs spell
                if ((cardNamePlayer1.equals("Kraken") && typePlayer2.equals("spell")) || (typePlayer1.equals("spell") && cardNamePlayer2.equals("Kraken"))) {
                    if (cardNamePlayer1.equals("Kraken")) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The kraken is immune to spells");
                        System.out.println("-> (" + player1Name + ") " + cardNamePlayer1 + " wins");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        System.out.println("The kraken is immune to spells");
                        System.out.println("-> (" + player2Name + ") " + cardNamePlayer2 + " wins");
                        adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
                    }
                    specialEvent = true;
                }

                //same element vs same element
                if(!specialEvent) {
                    if (elementPlayer1.equals(elementPlayer2)) {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2);
                    } else {
                        printBattle(cardNamePlayer1, cardNamePlayer2, damagePlayer1, damagePlayer2);
                        effectiveness(elementPlayer1, elementPlayer2, player1, player2, cardPlayer1, cardPlayer2);
                        printSpellAndMixedResults(damagePlayer1, damagePlayer2, cardNamePlayer1, cardNamePlayer2, player1, player2, cardPlayer1, cardPlayer2);
                    }
                }
            }
            System.out.println("Number of cards: " + player1.size());
            System.out.println("Number of cards: " + player2.size());
            System.out.println(numberOfGames);
            numberOfGames ++;
        }
        if(numberOfGames >= 100){
            System.out.println("Draw");
        }else{
            System.out.println("One player has won");
        }
    }

    public void printBattle(String cardNamePlayer1, String cardNamePlayer2, float damagePlayer1, float damagePlayer2) {
            System.out.println("Player 1 (" + player1Name + "): " + cardNamePlayer1 + " (" + damagePlayer1 + " Damage) vs Player 2 (" + player2Name + "): " + cardNamePlayer2 + " (" + damagePlayer2 + " Damage)");
    }

    public void printSpellAndMixedResults(float damagePlayer1, float damagePlayer2, String cardNamePlayer1, String cardNamePlayer2, ArrayList<BattleCards> player1, ArrayList<BattleCards> player2, int cardPlayer1, int cardPlayer2){
        if(damagePlayer1 == damagePlayer2){
            System.out.println("=> Damage: " + cardNamePlayer1 + ": " + damagePlayer1 + " vs " + cardNamePlayer2 + ": " +damagePlayer2);
            System.out.println("-> Draw");
        }
        if(damagePlayer1 > damagePlayer2){
            System.out.println("=> Damage: " + cardNamePlayer1 + ": " + damagePlayer1 + " vs " + cardNamePlayer2 + ": " +damagePlayer2);
            System.out.println("-> (" + player1Name + ") " + cardNamePlayer1 + " wins");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 1);
        }if(damagePlayer1 < damagePlayer2){
            System.out.println("=> Damage: " + cardNamePlayer1 + ": " + damagePlayer1 + " vs " + cardNamePlayer2 + ": " +damagePlayer2);
            System.out.println("-> (" + player2Name + ") " + cardNamePlayer2 + " wins");
            adjustDeck(player1, player2, cardPlayer1, cardPlayer2, 2);
        }
    }

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

    public void adjustDeck(ArrayList<BattleCards> player1, ArrayList<BattleCards> player2, int cardPlayer1, int cardPlayer2, int player){
        if(player == 1){
            player1.add(player2.get(cardPlayer2));
            player2.remove(cardPlayer2);
        }else if(player == 2){
            player2.add(player1.get(cardPlayer1));
            player1.remove(cardPlayer1);
        }
    }
}
