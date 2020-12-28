package Server;

public class RequestContext {
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
         System.out.println(requestString);
         if(request.equals("POST") || request.equals("PUT"))
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

}
