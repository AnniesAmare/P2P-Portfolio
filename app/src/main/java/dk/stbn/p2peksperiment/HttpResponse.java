package dk.stbn.p2peksperiment;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpResponse {
    String Header;
    String Status;
    String Body;

    public HttpResponse(String header, String status, String body){
        this.Header = header;
        this.Status = status;
        this.Body = body;

    }
    public HttpResponse(String header, String status){
        this.Header = header;
        this.Status = status;
        this.Body = "";
    }

    public HttpResponse(String jsonString){
        try {
            JSONObject json = new JSONObject(jsonString);
            this.Header = json.getString("header");
            this.Status = json.getString("status");
            this.Body = json.getString("body");

        }catch (JSONException e){
            System.out.println("Could not convert from: " + jsonString + " to HttpResponse");
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public String GetJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("header", this.Header);
            json.put("status", this.Status);
            json.put("body", this.Body);
        } catch (JSONException e) {
            System.out.println("Could not convert HttpResponse to json");
            return "null";
        }
        return json.toString();
    }

}
