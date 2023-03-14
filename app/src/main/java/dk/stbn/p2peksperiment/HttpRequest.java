package dk.stbn.p2peksperiment;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpRequest {
    String Header;
    String Method;
    String Path;
    String Body;

    public HttpRequest(String header, String method, String path, String body){
        this.Header = header;
        this.Path = path;
        this.Method = method;
        this.Body = body;
    }

    public HttpRequest(String header, String method, String path){
        this.Header = header;
        this.Path = path;
        this.Method = method;
        this.Body = "";
    }

    public HttpRequest(String jsonString){
        try {
            JSONObject json = new JSONObject(jsonString);
            this.Header = json.getString("header");
            this.Path = json.getString("path");
            this.Method = json.getString("method");
            this.Body = json.getString("body");

        }catch (JSONException e){
            System.out.println("Could not convert from: " + jsonString + " to HttpRequest");
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public String GetJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("header", this.Header);
            json.put("path", this.Path);
            json.put("method", this.Method);
            json.put("body", this.Body);

        } catch (JSONException e) {
            System.out.println("Could not convert HttpRequest to json");
            return null;
        }
        return json.toString();
    }



}
