package Server;//git repo: https://github.com/BlizzardLizzard/REST-HTTP-based-Server/

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {

    public static void main(String[] args) throws IOException {
        //starting the server here
        ServerSocket server = new ServerSocket(10001);
        System.out.println("Listening for connection on port 10001 ....");
        StartServer(server);
    }

    public  static void StartServer(ServerSocket server) throws IOException{
        try {
            while (true) {
                Socket socket = server.accept();
                InputStream inputStream = socket.getInputStream();

                //handles inputstream and saves it char by char
                StringBuilder result = new StringBuilder();
                while (inputStream.available() > 0) {
                    result.append((char) inputStream.read());
                }

                //string split by spaces into a String[]
                String request = result.toString();
                String[] requestSplit = request.split(" ");

                RequestHandler(requestSplit,socket, request);
                socket.close();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void RequestHandler(String[] requestSplit, Socket socket, String requestString) throws IOException {
        String request = requestSplit[0];
        if (!request.isEmpty()) {
            String[] httpVersion = requestSplit[2].split("\\r?\\n");
            //sends context of the request to the Server.RequestContext class to save important variables of teh request
            RequestContext requestContext =  new RequestContext(request, httpVersion[0], requestSplit[1], requestString);
            new RequestHandler(request, socket, requestContext);
        }
    }
}





