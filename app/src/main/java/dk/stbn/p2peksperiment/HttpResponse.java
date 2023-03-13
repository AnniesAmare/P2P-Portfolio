package dk.stbn.p2peksperiment;

public class HttpResponse {
    String Header;
    String Status;
    String Body;

    public HttpResponse(String header, String status, String body){
        this.Header = header;
        this.Status = status;
        this.Body = body;

    }


}
