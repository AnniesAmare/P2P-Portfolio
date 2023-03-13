package dk.stbn.p2peksperiment;

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

}
