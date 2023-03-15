package dk.stbn.p2peksperiment;

import android.annotation.SuppressLint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Singleton {
    private static Singleton instance;

    Thread server;

    ArrayList<NodeObserver> nodeObservers = new ArrayList<>();

    public static Singleton getInstance(){
        if (instance == null){
            instance = new Singleton();
            instance.startServer();
        }
        return instance;
    }

    private void startServer(){
        Thread serverThread = new Thread(new MyServerThread());
        this.server = serverThread;
        this.server.start();
    }

    public void notifyNode(String info){
        for (NodeObserver nObserver : nodeObservers){
            nObserver.update(info);
        }
    }

    public void addObserver(NodeObserver newNodeObserver){
        nodeObservers.add(newNodeObserver);
    }

    public void removeObserver(NodeObserver oldNodeObserver){
        nodeObservers.add(oldNodeObserver);
    }
    class MyServerThread implements Runnable {
        @SuppressLint("SuspiciousIndentation")
        @Override
        public void run() {
            //Always be ready for next client
            //This is a test
            boolean running = true;
            while (true) {
                try {
                    while (running) {
                        try {
                            ServerSocket serverSocket = new ServerSocket(4444);
                            notifyNode("SERVER: start listening..");
                            Socket nodeSocket = serverSocket.accept();
                            notifyNode("SERVER connection accepted");

                            DataInputStream inNodeStream = new DataInputStream(nodeSocket.getInputStream());
                            DataOutputStream outNodeStream = new DataOutputStream(nodeSocket.getOutputStream());

                            String request;
                            String response;

                            //default response
                            HttpResponse defaultHttpResponse = new HttpResponse("HTTP", "404 Not Found");
                            response = defaultHttpResponse.GetJsonString();

                            //serverCarryOn = true;
                            //Start conversation
                            while (true) {
                                try {
                                    request = (String) inNodeStream.readUTF();

                                    notifyNode("Client says: " + request);
                                    //notify(X)

                                    System.out.println("client to server " + request);

                                    //Converting request to a HttpRequest object
                                    HttpRequest httpRequest = new HttpRequest(request);
                                    //If the request is successfully read:
                                    if (!httpRequest.Path.isEmpty()) {
                                        HttpResponse httpResponse;
                                        //defining the response based on the path
                                        switch(httpRequest.Path.toLowerCase()){
                                            case "getid":
                                                httpResponse = new HttpResponse("HTTP", "200 OK", "node.getId()");
                                                break;

                                            case "getneighbors":
                                                httpResponse = new HttpResponse("HTTP", "200 OK", "node.getneighbors");
                                                break;

                                            default:
                                                System.out.println("Does not recognize path: " + httpRequest.Path.toLowerCase());
                                                httpResponse = new HttpResponse("HTTP", "400 Bad Request");
                                        }

                                        response = httpResponse.GetJsonString();
                                    }

                                    notifyNode(response);
                                    outNodeStream.writeUTF(response);
                                    outNodeStream.flush();
                                    waitABit();
                                    //serverCarryOn = false;

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw new RuntimeException(e);
                                }
                            }
                        } catch (IOException e) {
                            notifyNode("ServerCarryOn encountered an error");
                            //serverCarryOn = false;
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                } catch (Exception e) {
                    running = false;
                }
            }//While loop
        }//run
    }//runnable


    private void waitABit() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}


