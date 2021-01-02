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
                case "DELETE" -> DeleteRequest(requestContext, out);
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

        if(requestContext.URI.startsWith("/users") || requestContext.URI.equals("/sessions")) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestContext.message);
            String username = jsonNode.get("Username").asText();
            String password = jsonNode.get("Password").asText();

            if (requestContext.URI.startsWith("/users")) {
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
                            System.out.println("ID: " + resultSet.getString(1) + " Name: " + resultSet.getString(2) + " Damage: " + resultSet.getFloat(3));
                            PreparedStatement insert = con.prepareStatement("INSERT INTO stack(id, name, damage, player) VALUES (?,?,?,?)");
                            insert.setString(1, resultSet.getString(1));
                            insert.setString(2, resultSet.getString(2));
                            insert.setFloat(3, resultSet.getFloat(3));
                            insert.setString(4, token);
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
                contentLength = stringRequest.length();
                PrintReply(out);
                out.println(stringRequest);
                out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }

        /*try {
            //looks at number of entries in a directory and saves the amount of entries as an int and creates a file with that int as name
            int lastFileNumber = 0;
            numberOfEntriesInDir = Objects.requireNonNull(new File("messages/").listFiles()).length;
            numberOfEntriesInDir += 1;
            File postFile = new File("messages/" + numberOfEntriesInDir + ".txt");

            //checks for the highest ID in the directory to prevent overwriting
            File files = new File("messages/");
            String[] filesInDir = files.list();
            int currentNumberOfEntriesInDir = filesInDir.length;
            if(currentNumberOfEntriesInDir > 0){
                String[] lastFileName = filesInDir[currentNumberOfEntriesInDir-1].split("\\.");
                lastFileNumber = Integer.parseInt(lastFileName[0]);
            }
            //checks if file exists and if not writes the content in the corresponding txt
            if(!postFile.exists() && lastFileNumber < numberOfEntriesInDir){
                FileWriter writer = new FileWriter(postFile);
                writer.write(requestContext.getMessage());
                writer.close();
                status = "201";
                stringRequest = "ID: " + numberOfEntriesInDir;
            }else{
                lastFileNumber++;
                File newFile = new File("messages/" + lastFileNumber + ".txt");
                FileWriter newFileWriter = new FileWriter(newFile);
                newFileWriter.write(requestContext.getMessage());
                newFileWriter.close();
                status = "201";
                stringRequest = "ID: " + lastFileNumber;
            }
            contentLength = stringRequest.length();
            PrintReply(out);
            out.println(stringRequest);
            out.flush();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }*/
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

        /*String path = requestContext.getURI();
        String[] pathSplit = path.split("/");
        File getFile = new File("messages/");
        String[] pathNames = getFile.list();
        int numberOfStrings = pathSplit.length;

        //if the path that was given without a number parse through the directory and print names and messages from the files in the directory
        if(numberOfStrings <= 2 && pathNames.length > 0){
            status = "200";
            //for every entry in the directory the name and message is read and printed
            for(String pathname : pathNames){
                File file = new File("messages/" + pathname);
                Scanner reader = new Scanner(file);
                String message = " ";
                while(reader.hasNextLine()){
                    message = reader.nextLine();
                }
                stringRequest += "Entry: " + pathname + " Message: " + message + "\r\n";
                reader.close();
            }
            contentLength = stringRequest.length();
            PrintReply(out);
            out.println(stringRequest);
        }else if(numberOfStrings > 2){
            //if a number is given search the directory for said name and read from it if it exists
            File getRequest = new File("messages/" + pathSplit[2] + ".txt");
            if(getRequest.exists()){
                status = "200";
                Scanner reader = new Scanner(getRequest);
                String message = " ";
                while(reader.hasNextLine()){
                    message = reader.nextLine();
                }
                reader.close();
                stringRequest = "Message from ID " + pathSplit[2] + ": " + message;
            }else{
                status = "404";
                stringRequest = "Message ID doesn't exist!";
            }
            contentLength = stringRequest.length();
            PrintReply(out);
            out.println(stringRequest);
        }else{
            status = "400";
            stringRequest = "Folder is empty!";
            contentLength = stringRequest.length();
            PrintReply(out);
            out.println(stringRequest);
        }
        out.flush();*/
    }

    //handles the DELETE requests
    public void DeleteRequest(RequestContext requestContext, PrintWriter out){
        String path = requestContext.getURI();
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
            }
        contentLength = stringRequest.length();
        PrintReply(out);
        out.println(stringRequest);
        out.flush();
    }

    //handles the PUT requests
    public void PutRequest(RequestContext requestContext, PrintWriter out, Connection con) throws IOException, SQLException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        int numberOfCards = jsonNode.size();

        if(numberOfCards == 4) {

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



        /*String path = requestContext.getURI();
        String[] pathSplit = path.split("/");
        int numberOfStrings = pathSplit.length;

        if (numberOfStrings > 2) {
            File file = new File("messages/" + pathSplit[2] + ".txt");
            if (file.exists()) {
                FileWriter writer = new FileWriter(file);
                //overwrites the message that has been inside the wanted txt
                writer.write(requestContext.getMessage());
                writer.close();
                status = "200";
                stringRequest = "File " + pathSplit[2] + ".txt updated!";
            } else {
                status = "404";
                stringRequest = "Please enter a valid ID!";
            }
        }else{
            status = "400";
            stringRequest = "Please enter an ID!";
        }
        contentLength = stringRequest.length();
        PrintReply(out);
        out.println(stringRequest);
        out.flush();*/
    }

    //responsible for printing the header of the reply
    public void PrintReply(PrintWriter out){
        out.println(httpVersion + " " + status);
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + contentLength);
        out.println("");
    }
}


