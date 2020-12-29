package Server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Scanner;

public class RequestHandler {
    public String request;
    public int numberOfEntriesInDir = 0;
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
                case "GET" -> GetRequest(requestContext, out);
                case "DELETE" -> DeleteRequest(requestContext, out);
                case "PUT" -> PutRequest(requestContext, out);
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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(requestContext.message);
        String username = jsonNode.get("Username").asText();
        String password = jsonNode.get("Password").asText();

        if(requestContext.URI.equals("/users")) {
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
                }else{
                    status = "409";
                    stringRequest = "User already exists";
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
    public void GetRequest(RequestContext requestContext, PrintWriter out) throws FileNotFoundException {
        String path = requestContext.getURI();
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
        out.flush();
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
    public void PutRequest(RequestContext requestContext, PrintWriter out) throws IOException {
        String path = requestContext.getURI();
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
        out.flush();
    }

    //responsible for printing the header of the reply
    public void PrintReply(PrintWriter out){
        out.println(httpVersion + " " + status);
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + contentLength);
        out.println("");
    }
}


