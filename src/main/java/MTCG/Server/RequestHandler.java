package MTCG.Server;

import MTCG.BattleHandler;
import MTCG.CardHandler;
import MTCG.ShopHandler;
import MTCG.UserHandler;

import java.net.Socket;

public class RequestHandler {

    //filters the incoming requests to the right function needed
    public RequestHandler(Socket socket, RequestContext requestContext) {
        try {
            String[] UriCase = requestContext.URI.split("/");
            switch (UriCase[1]) {
                case "users", "sessions", "stats", "score", "log" -> new UserHandler(requestContext, socket);
                case "packages", "transactions", "tradings" -> new ShopHandler(requestContext, socket);
                case "cards", "deck", "deck?format=plain" -> new CardHandler(requestContext, socket);
                case "battles" -> new BattleHandler(requestContext, socket);
                default -> {
                    ReplyHandler replyHandler = new ReplyHandler(socket);
                    replyHandler.generalErrorReply();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}