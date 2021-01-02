package Server;

public class RequestContext {

    public String requestString;
    public String request;
    public String HttpType;
    public String URI;
    public String message;

     //class to save important variables to use later
     public RequestContext(String request, String HttpType, String URI, String requestString)
     {
         this.request = request;
         this.HttpType = HttpType;
         this.URI = URI;
         this.requestString = requestString;
         System.out.println(requestString);
         int numberOfURIParts = URI.split("/").length;
         if((request.equals("POST") || request.equals("PUT")) && numberOfURIParts == 2)
         {
             messageHandler(requestString);
         }
     }
    public String getRequest() {
        return request;
    }

    public String getHttpType() {
        return HttpType;
    }

    public String getMessage() {
        return message;
    }

    public String getURI() {
        return URI;
    }

    public void messageHandler(String requestString){
         String[] lines = requestString.split("\\r?\\n");
         int i = 0;
         while(!(lines[i].length() == 0)){
                i++;
         }
        message = lines[i+1];
    }

    public String authenticationToken(String requestString) {
        String[] lines = requestString.split("\\r?\\n");
        int i = 0;
        if (requestString.contains("Authorization:")) {
            while (!(lines[i].startsWith("Authorization:"))) {
                i++;
            }
            String realToken[] = lines[i].split(": ");
            return realToken[1];
        }
        return null;
    }
}
