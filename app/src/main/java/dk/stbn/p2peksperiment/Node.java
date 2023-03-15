package dk.stbn.p2peksperiment;

import java.util.List;
import java.util.*; //import for dictionary

//https://www.javatpoint.com/dictionary-class-in-java

public class Node {
    List<String> NodesLeft;
    List<String> NodesRight;

    String Id;

    Dictionary DataStorage;
    List<String> DataIndex; //should be directory
    List<String> Data;


    public Node(String ip, List<String> nodesLeft, List<String> nodesRight){
        Id = ip;
        NodesLeft = nodesLeft;
        NodesRight = nodesRight;
        DataStorage = new Hashtable();

    }


    public String getId(){
        return Id;
    }

    public List<String> newNeighbor(List<String> newNeighborIds, String side){
        if(side.equals("left")){
            NodesLeft.addAll(newNeighborIds);
            return NodesLeft;
        }else{
            NodesRight.addAll(newNeighborIds);
            return NodesRight;
        }
    }

    public List<String> GetPhonebookLeft(){
        return NodesLeft; //returns nodes left or right;
    }

    public List<String> GetPhonebookRight(){
        return NodesRight; //returns nodes left or right;
    }



    /*
    String GetData(String dataId){
        for (int i = 0; i < DataIndex.size(); i++) {
            if(dataId.equals(DataIndex.get(i))){
                return Data.get(i);
            }
        }
        return null;
    }

    void RemoveData(String dataId){
        for (int i = 0; i < DataIndex.size(); i++) {
            if(dataId.equals(DataIndex.get(i))){
                Data.remove(i);
                break;
            }
        }
    }


     */

    public String AddData(String newData){
        //hashes the the data. The hashed data is kept as key
        SHA256 newHash = new SHA256();
        String dataId = newHash.hash(newData);

        //create new data object
        Data data = new Data(dataId,newData,true,false);

        //convert data to json to store it
        String dataAsJson = data.DataToJson();

        //add data to storage
        this.DataStorage.put(data.ID,dataAsJson);

        return dataId;

    }




}
