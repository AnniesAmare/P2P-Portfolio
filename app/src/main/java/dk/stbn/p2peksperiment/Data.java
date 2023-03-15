package dk.stbn.p2peksperiment;
import org.json.JSONException;
import org.json.JSONObject;


public class Data {
    String ID;
    String Value;
    boolean IsParent;
    boolean IsGlobal;


    public Data(String ID, String Value){
        this.ID = ID;
        this.Value = Value;
        this.IsParent = true;
        this.IsGlobal = false;
    }

    public Data(String ID, String Value, boolean parent, boolean global){
        this.ID = ID;
        this.Value = Value;
        this.IsParent = parent;
        this.IsGlobal = global;
    }


    //json to data object
    public Data(String jsonString){
        try {
            JSONObject json = new JSONObject(jsonString);
            this.ID = json.getString("id");
            this.Value = json.getString("value");
            this.IsParent = json.getBoolean("isparent");
            this.IsGlobal = json.getBoolean("isglobal");

        }catch (JSONException e){ //the error is thrown here
            System.out.println("Could not convert from: " + jsonString + " to Data");
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }


    public String DataToJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", this.ID);
            json.put("value", this.Value);
            json.put("isparent", this.IsParent);
            json.put("isglobal", this.IsGlobal);

        } catch (JSONException e) {
            System.out.println("Could not convert HttpRequest to json");
            return null;
        }
        return json.toString();
    }





}
