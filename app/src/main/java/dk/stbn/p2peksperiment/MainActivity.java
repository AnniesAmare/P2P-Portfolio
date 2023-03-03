package dk.stbn.p2peksperiment;



import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // UI-elements
    private Button startClient, startServer, serverSend, clientSend;
    private TextView serverInfoTv, clientInfoTv;
    private EditText serverMessageField, clientMessageField;

    // Logging/status messages
    private String serverinfo = "SERVER LOG:";
    private String clientinfo = "CLIENT LOG: ";
    //Global data
    private final int PORT = 4444;
    private String THIS_IP_ADDRESS = "";
    private String REMOTE_IP_ADDRESS = "";
    private ArrayList<String> ips = new ArrayList();

    private String theMessage = ""; //Where user input is stored
    private Thread serverThread = new Thread(new MyServerThread());
    private Thread clientThread = new Thread(new MyClientThread());

    //---Some state---
    private boolean ip_submitted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI boilerplate
        startClient = findViewById(R.id.button);
        startServer = findViewById(R.id.button2);

        serverInfoTv = findViewById(R.id.serveroutput);
        clientInfoTv = findViewById(R.id.clientoutput);
        serverSend = findViewById(R.id.sendserver);
        clientSend = findViewById(R.id.sendclient);

        startClient.setOnClickListener(this);
        startServer.setOnClickListener(this);
        serverSend.setOnClickListener(this);
        clientSend.setOnClickListener(this);
        serverSend.setEnabled(false); //Disable Send button until connection is established
        clientSend.setEnabled(false);
        serverMessageField = findViewById(R.id.servermessagefield);
        serverMessageField.setHint("Press START SERVER");
        clientMessageField = findViewById(R.id.clientmessagefield);
        clientMessageField.setHint("Press START CLIENT");
        addHardcodedIPs();
    }




    private void addHardcodedIPs() {
        ips.add("192.168.10.106");//Sune home
        ips.add("192.168.10.187");//Sune home
        ips.add("192.168.10.118");//Sune home
        ips.add("10.90.17.158");//RucIOT

    }

    @Override
    public void onClick(View view) {

        //  - - Client - -
        if(view == startClient) {
            if(!ip_submitted){
                cUpdate("Enter server IP-address");
                clientMessageField.setHint("Enter server-IP-address");
                startClient.setEnabled(false);
                clientSend.setText("submit IP");
                clientSend.setEnabled(true);
                return;
            }

            clientMessageField.setHint("Write something");
            clientThread.start();
            startClient.setEnabled(false); //Don't start two clients
            clientinfo += "- - - CLIENT STARTED - - - \n";

        }
        else if(view == clientSend){
            theMessage = clientMessageField.getText().toString();

            if (!ip_submitted){
                ip_submitted = true;
                clientSend.setText("send");
                REMOTE_IP_ADDRESS = theMessage;
                startClient.setEnabled(true);
                clientMessageField.setHint("Now press START CLIENT");
                clientSend.setEnabled(true);
                return;
            }
            clientMessageField.setText("");
            clientMessageField.setHint("type message here...");
            synchronized (clientThread) {

                clientThread.notify();

            }
            clientSend.setEnabled(false);
        }

        //  - - Server - -

        else if (view == startServer){
            serverThread.start();
            serverinfo += "- - - SERVER STARTED - - -\n";

            //Obtain my local IP-address
            try {
                THIS_IP_ADDRESS = getLocalIpAddress();
                sUpdate("SERVER: Automatic SERVER IP: " + THIS_IP_ADDRESS);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            startServer.setEnabled(false); //Don't start two servers
            serverSend.setEnabled(false); //Client initiates conversation
            serverMessageField.setHint("Wait for client to initiate");
        }
        else if (view == serverSend){

            theMessage = serverMessageField.getText().toString();
            serverMessageField.setText("");
            serverMessageField.setHint("type message here...");




                synchronized (serverThread) {
                    //update("onclick notify server");
                    serverThread.notify();
                }




        }


    }//onclick

    class MyServerThread implements Runnable {
        @Override
        public void run() {
            //Alwway be ready for next client
            while (true) {

                try {
                    //sUpdate("SERVER: starting serversocket");
                    ServerSocket serverSocket = new ServerSocket(4444);
                    sUpdate("SERVER: start listening..");
                    Socket klientSocket = serverSocket.accept();
                    sUpdate("SERVER connection accepted");

                    DataInputStream instream = new DataInputStream(klientSocket.getInputStream());
                    DataOutputStream outstream = new DataOutputStream(klientSocket.getOutputStream());

                    //Start conversation
                    boolean carryOn = true;
                    while(carryOn) {

                        String str = (String) instream.readUTF();
                        sUpdate("Client says: " + str);
                        serverButtonEnable(true);
                        synchronized (serverThread){
                            serverThread.wait(); //Waiting for user input
                        }
                        String answer = theMessage;
                        outstream.writeUTF(answer);
                        outstream.flush();
                        carryOn = !str.equalsIgnoreCase("bye");
                    }
                    //Closing everything down
                    serverSocket.close();
                    sUpdate("SERVER: Server socket closed");
                    klientSocket.close();
                    sUpdate("SERVER: Client socket closed");
                    instream.close();
                    sUpdate("SERVER: inputstream closed");
                    outstream.close();
                    sUpdate("SERVER: outputstream closed");
                } catch (IOException e) {
                    sUpdate("oops!!");
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

//Copied from https://www.tutorialspoint.com/sending-and-receiving-data-with-sockets-in-android
private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }
    class MyClientThread implements Runnable {
        @Override
        public void run() {

            try {
                cUpdate("CLIENT: starting client socket ");
                Socket connectionToServer = new Socket(REMOTE_IP_ADDRESS, 4444);
                cUpdate("CLIENT: client connected ");

                DataInputStream instream = new DataInputStream(connectionToServer.getInputStream());
                //cUpdate(""+instream);
                DataOutputStream out = new DataOutputStream(connectionToServer.getOutputStream());
                //cUpdate("CLIENT: made outputstream");
                boolean carryOn = true;
                while(carryOn) {
                    clientButtonEnable(true);
                    synchronized (clientThread) {
                    try{
                        //cUpdate("Waiting for your message...");
                        clientThread.wait();
                        //cUpdate("after wait");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    }
                    String besked = theMessage;
                    out.writeUTF(besked);
                    //cUpdate("CLIENT: wrote to outputstream");
                    out.flush();
                    //cUpdate("CLIENT: flushed");
                    String messageFromServer = instream.readUTF();
                    cUpdate("Server says: " +messageFromServer);

                    carryOn = !messageFromServer.equalsIgnoreCase("bye");
                }
                instream.close();
                cUpdate("CLIENT: closed inputstream");
                out.close();
                cUpdate("CLIENT: closed outputstream");
                connectionToServer.close();
                cUpdate("CLIENT: closed socket");

            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }//run()
    } //class MyClientThread

    //The below four methods are for updating UI-elements on the main thread
    public void sUpdate(String message){
        //Run this code on UI-thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                serverinfo = message + "\n" + serverinfo;
                serverInfoTv.setText(serverinfo);
            }
        });

    }
    public void cUpdate(String message){
        System.out.println(message);

        //Run this code on UI-thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                clientinfo = message + "\n" + clientinfo;
                clientInfoTv.setText(clientinfo);
            }
        });

    }
    void serverButtonEnable(boolean enabled){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                serverSend.setEnabled(enabled);
            }
        });
    }

    void clientButtonEnable(boolean enabled){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                clientSend.setEnabled(enabled);
            }
        });
    }

    public String getAnimal() {
        int max = animals.length;

        int r = (int) Math.random()*max+1;
        return animals[r];
    }
    public String getFood() {
        int max = food.length;
        int min = 0;
        int r = (int) Math.random()*max+1;
        return food[r];
    }
    String[] animals = { //from https://www.enchantedlearning.com/
            "aardvark",
            "abalone",
            "African elephant",
            "African gray parrot",
            "African penguin",
            "African rock python",
            "African wild cat",
            "agouti",
            "airedale terrier",
            "Alaskan malamute",
            "albatross",
            "algae",
            "alligator",
            "alpaca",
            "American bison",
            "American cocker spaniel",
            "American crocodile",
            "American flamingo",
            "American golden plover",
            "American Robin",
            "American tree sparrow",
            "amoeba",
            "amphibian",
            "anaconda",
            "angelfish",
            "angelshark",
            "angonoka",
            "animal",
            "anole",
            "ant",
            "anteater",
            "antelope",
            "Apatosaurus",
            "ape",
            "aphid",
            "arachnid",
            "Archaeopteryx",
            "arctic fox",
            "Arctic tern",
            "arctic wolf",
            "armadillo",
            "Arsinoitherium",
            "arthropod",
            "artiodactyls",
            "asp",
            "assassin bug",
            "aye-aye",
            "baboon",
            "bactrian camel",
            "badger",
            "bald eagle",
            "bandicoot",
            "barnacle",
            "barracuda",
            "basilisk",
            "basking shark",
            "bass",
            "basset hound",
            "bat",
            "beagle",
            "bear",
            "bearded dragon",
            "beaver",
            "bed bug",
            "bee",
            "beetle",
            "beluga whale",
            "bichon frise",
            "bighorn sheep",
            "bilby",
            "binturong",
            "bird",
            "bison",
            "bivalve",
            "black bear",
            "black bear hamster",
            "black caiman",
            "black racer",
            "black swan",
            "blackbird",
            "bloodhound",
            "blowfish",
            "blue jay",
            "blue morpho butterfly",
            "blue ring octopus",
            "blue shark",
            "blue whale",
            "blue-tongued skink",
            "bluebird",
            "bluefin tuna",
            "boa constrictor",
            "bobcat",
            "bongo",
            "bonobo",
            "bony fish",
            "border collie",
            "Boston terrier",
            "bowhead whale",
            "box turtle",
            "boxer",
            "brittle star",
            "brown bear",
            "brown pelican",
            "buffalo",
            "bug",
            "bull",
            "bull shark",
            "bull snake",
            "bulldog",
            "bullfrog",
            "bumblebee",
            "bushbaby",
            "butterfly",
            "caiman",
            "California sea lion",
            "camel",
            "Canada goose",
            "canary",
            "cape buffalo",
            "Cape hunting dog",
            "capybera",
            "caracal",
            "cardinal",
            "caribou",
            "carnivora",
            "carpenter ant",
            "cassowary",
            "cat",
            "catamount",
            "caterpillar",
            "cattle",
            "cavy",
            "centipede",
            "cephalpod",
            "chameleon",
            "cheetah",
            "chickadee",
            "chicken",
            "chihuahua",
            "chimipanzee",
            "chinchilla",
            "chipmunk",
            "chiton",
            "chrysalis",
            "cicada",
            "clam",
            "clownfish",
            "coati",
            "cobra",
            "cockatoo",
            "cockroach",
            "cod",
            "coelacanth",
            "collared lizard",
            "collared peccary",
            "collie",
            "colugo",
            "common rhea",
            "companion dog",
            "conch",
            "cookiecutter shark",
            "copepod",
            "copperhead snake",
            "coral",
            "coral snake",
            "corn snake",
            "cottonmouth",
            "cougar",
            "cow",
            "coyote",
            "coypu",
            "crab",
            "crane",
            "crayfish",
            "cricket",
            "crocodile",
            "crow",
            "crustacean",
            "Cryptoclidus",
            "cuttlefish",
            "cutworm",
            "Dachshund",
            "dall sheep",
            "Dall's porpoise",
            "Dalmatian",
            "damselfly",
            "dark-eyed junco",
            "darkling beetle",
            "deer",
            "Deinonychus",
            "desert tortoise",
            "Desmatosuchus",
            "dhole",
            "diatom",
            "Dilophosaurus",
            "Dimetrodon",
            "dingo",
            "Dinichthys",
            "Dinornis",
            "dinosaur",
            "Diplodocus",
            "Doberman pinscher",
            "dodo",
            "Doedicurus",
            "dog",
            "dogfish",
            "dolphin",
            "dolphin, bottlenose",
            "dolphin, spotted",
            "donkey",
            "dory",
            "dove",
            "downy woodpecker",
            "dragonfly",
            "dromedary",
            "duck",
            "duck-billed platypus",
            "dugong",
            "dung beetle",
            "Dunkleosteus",
            "eagle",
            "earthworm",
            "earwig",
            "eastern bluebird",
            "eastern quoll",
            "echidna",
            "echinoderms",
            "Edenta",
            "Edmontonia",
            "Edmontosaurus",
            "eel",
            "egg",
            "egret",
            "ekaltadelta",
            "eland",
            "Elasmosaurus",
            "Elasmotherium",
            "electric eel",
            "elephant",
            "elephant seal",
            "elk",
            "emerald tree boa",
            "emperor angelfish",
            "emperor penguin",
            "emu",
            "endangered species",
            "Eohippus",
            "Eoraptor",
            "ermine",
            "Estemmenosuchus",
            "extinct animals",
            "Fabrosaurus",
            "falcon",
            "farm animals",
            "fennec fox",
            "ferret",
            "fiddler crab",
            "fin whale",
            "finch",
            "fireant",
            "firefly",
            "fish",
            "flamingo",
            "flatworm",
            "flea",
            "flightless birds",
            "flounder",
            "fly",
            "flying fish",
            "flying squirrel",
            "forest antelope",
            "forest giraffe",
            "fossa",
            "fowl",
            "fox",
            "frilled lizard",
            "frog",
            "fruit bat",
            "fruit fly",
            "fugu",
            "galagos",
            "Galapagos shark",
            "gar",
            "gastropod",
            "gavial",
            "gazelle",
            "gecko",
            "gerbil",
            "German shepherd",
            "giant squid",
            "gibbon",
            "gila monster",
            "giraffe",
            "Glyptodon",
            "gnat",
            "gnu",
            "goat",
            "golden eagle",
            "golden lion tamarin",
            "golden retriever",
            "goldfinch",
            "goldfish",
            "goose",
            "gopher",
            "gorilla",
            "grasshopper",
            "gray whale",
            "great apes",
            "great Dane",
            "great egret",
            "great horned owl",
            "great white shark",
            "green darner dragonfly",
            "green iguana",
            "Greenland shark",
            "greyhound",
            "grizzly bear",
            "groundhog",
            "grouper",
            "grouse",
            "grub",
            "guinea pig",
            "gull",
            "gulper eel",
            "hammerhead shark",
            "hamster",
            "hare",
            "harlequin bug",
            "harp seal",
            "harpy eagle",
            "hatchetfish",
            "Hawaiian goose",
            "hawk",
            "hedgehog",
            "hen",
            "hermit crab",
            "heron",
            "herring",
            "hippo",
            "hippopotamus",
            "honey bee",
            "hornet",
            "horse",
            "horseshoe crab",
            "hound",
            "house fly",
            "howler monkey",
            "human being",
            "hummingbird",
            "humpback whale",
            "husky",
            "hyena",
            "Hyracotherium",
            "hyrax",
            "ibis",
            "Ichthyornis",
            "Ichthyosaurus",
            "iguana",
            "Iguanodon",
            "imago",
            "impala",
            "Indian elephant",
            "insect",
            "insectivores",
            "invertebrates",
            "Irish setter",
            "isopod",
            "jack rabbit",
            "Jack Russell terrier",
            "jaguar",
            "Janenschia",
            "Japanese crane",
            "javelina",
            "jay",
            "jellyfish",
            "jerboa",
            "joey",
            "John Dory",
            "jumping bean moth",
            "junco",
            "junebug",
            "kakapo",
            "kangaroo",
            "kangaroo rat",
            "karakul",
            "katydid",
            "keel-billed toucan",
            "Kentrosaurus",
            "killer whale",
            "king cobra",
            "king crab",
            "kinkajou",
            "kiwi",
            "knobbed whelk",
            "koala",
            "Komodo dragon",
            "kookaburra",
            "krill",
            "Kronosaurus",
            "Kudu",
            "Labrador retriever",
            "ladybug",
            "lagomorph",
            "lake trout",
            "lanternfish",
            "larva",
            "leafcutter ant",
            "leghorn",
            "lemming",
            "lemon shark",
            "lemur",
            "leopard",
            "Lhasa apso",
            "lice",
            "lightning bug",
            "limpet",
            "lion",
            "Liopleurodon",
            "lizard",
            "llama",
            "lobster",
            "locust",
            "loggerhead turtle",
            "longhorn",
            "loon",
            "lorikeet",
            "loris",
            "louse",
            "luminous shark",
            "luna moth",
            "lynx",
            "macaque",
            "macaw",
            "mackerel",
            "Macrauchenia",
            "maggot",
            "mako shark",
            "mallard duck",
            "mamba",
            "mammal",
            "mammoth",
            "man-o'-war",
            "manatee",
            "mandrill",
            "manta ray",
            "mantid",
            "mantis",
            "marbled murrelet",
            "marine mammals",
            "marmoset",
            "marmot",
            "marsupial",
            "mastiff",
            "mastodon",
            "meadowlark",
            "mealworm",
            "meerkat",
            "Megalodon",
            "megamouth shark",
            "merganser",
            "mice",
            "midge",
            "migrate",
            "millipede",
            "mink",
            "minnow",
            "moa",
            "mockingbird",
            "mole",
            "mollusk",
            "monarch butterfly",
            "mongoose",
            "monitor lizard",
            "monkey",
            "monotreme",
            "moose",
            "moray eel",
            "Morganucodon",
            "morpho butterfly",
            "mosquito",
            "moth",
            "mountain lion",
            "mouse",
            "mudpuppy",
            "musk ox",
            "muskrat",
            "mussels",
            "mustelids",
            "nabarlek",
            "naked mole-rat",
            "nandu",
            "narwhal",
            "nautilus",
            "nene",
            "nest",
            "newt",
            "nightingale",
            "nine-banded armadillo",
            "North American beaver",
            "North American porcupine",
            "northern cardinal",
            "northern elephant seal",
            "northern fur seal",
            "northern right whale",
            "numbat",
            "nurse shark",
            "nuthatch",
            "nutria",
            "nymph",
            "ocelot",
            "octopus",
            "okapi",
            "old English sheepdog",
            "onager",
            "opossum",
            "orangutan",
            "orca",
            "Oregon silverspot butterfly",
            "oriole",
            "Ornitholestes",
            "Ornithomimus",
            "oropendola",
            "Orthacanthus",
            "oryx",
            "ostrich",
            "otter, river",
            "otter, sea",
            "Oviraptor",
            "owl",
            "ox",
            "oxpecker",
            "oyster",
            "painted lady butterfly",
            "painted turtle",
            "panda",
            "pangolin",
            "panther",
            "parakeet",
            "parrot",
            "peacock",
            "peafowl",
            "pekingese",
            "pelican",
            "penguin",
            "peregrine falcon",
            "Perissodactyls",
            "petrel",
            "pig",
            "pigeon",
            "pika",
            "pill bug",
            "pinnipeds",
            "piranha",
            "placental mammals",
            "plankton",
            "platybelodon",
            "platypus",
            "ploughshare tortoise",
            "plover",
            "polar bear",
            "polliwog",
            "pomeranian",
            "pompano",
            "pond skater",
            "poodle",
            "porcupine",
            "porpoise",
            "Port Jackson shark",
            "Portuguese water dog",
            "Postosuchus",
            "prairie chicken",
            "praying mantid",
            "praying mantis",
            "primates",
            "Proboscideans",
            "pronghorn",
            "protozoan",
            "pufferfish",
            "puffin",
            "pug",
            "puma",
            "pupa",
            "pupfish",
            "python",
            "Quaesitosaurus",
            "quagga",
            "quail",
            "Queen Alexandra's birdwing",
            "queen conch",
            "quetzal",
            "quokka",
            "quoll",
            "rabbit",
            "raccoon",
            "rat",
            "rattlesnake",
            "ray",
            "red hooded duck",
            "red kangaroo",
            "red panda",
            "red wolf",
            "red-tailed hawk",
            "redbilled oxpecker",
            "reindeer",
            "reptile",
            "rhea",
            "rhino",
            "rhinoceros",
            "Rhode Island red",
            "right whale",
            "ring-billed gull",
            "ring-tailed lemur",
            "ringtail possum",
            "river otter",
            "roach",
            "roadrunner",
            "robin",
            "rock dove",
            "rockhopper penguin",
            "rodent",
            "rooster",
            "rottweiler",
            "roundworm",
            "ruby-throated hummingbird",
            "sailfish",
            "salamander",
            "salmon",
            "sand dollar",
            "sandpiper",
            "scallop",
            "scarlet macaw",
            "scorpion",
            "Scottish terrier",
            "sea anemone",
            "sea cow",
            "sea cucumber",
            "sea otter",
            "sea star",
            "sea turtle",
            "sea urchin",
            "sea worm",
            "seahorse",
            "seal",
            "sealion",
            "serval",
            "shark",
            "sheep",
            "shrew",
            "shrimp",
            "siamang",
            "Siberian husky",
            "silkworm",
            "silverfish",
            "skink",
            "skipper",
            "skunk",
            "sloth",
            "slow worm",
            "slug",
            "Smilodon",
            "snail",
            "snake",
            "snapper",
            "snapping turtle",
            "snow goose",
            "snow leopard",
            "snowy owl",
            "softshell turtle",
            "sparrow",
            "spectacled caiman",
            "spectacled porpoise",
            "spider",
            "spiny anteater",
            "spiny lizard",
            "sponge",
            "spotted owl",
            "springtail",
            "squid",
            "squirrel",
            "St. Bernard",
            "starfish",
            "starling",
            "Stegosaurus",
            "stingray",
            "stonefly",
            "stork",
            "sugar glider",
            "sunfish",
            "swallowtail butterfly",
            "swan",
            "swift",
            "swordfish",
            "T. rex",
            "tadpole",
            "tamarin",
            "tanager",
            "tapir",
            "tarantula",
            "tarpon",
            "tarsier",
            "Tasmanian devil",
            "Tasmanian tiger",
            "Teratosaurus",
            "termite",
            "tern",
            "terrier",
            "Thecodontosaurus",
            "Thescelosaurus",
            "three-toed sloth",
            "thresher shark",
            "thrip",
            "tick",
            "tiger",
            "tiger shark",
            "tiger swallowtail butterfly",
            "toad",
            "Torosaurus",
            "tortoise",
            "toucan",
            "Trachodon",
            "tree shrew",
            "tree sparrow",
            "treefrog",
            "Triceratops",
            "Trilobite",
            "Troodon",
            "trout",
            "trumpeter swan",
            "tsetse fly",
            "tuatara",
            "tuna",
            "tundra wolf",
            "turkey",
            "turtle",
            "Tyrannosaurus rex",
            "Ultrasaurus",
            "Ulysses butterfly",
            "umbrellabird",
            "ungulates",
            "uniramians",
            "urchin",
            "Utahraptor",
            "valley quail",
            "vampire bat",
            "veiled chameleon",
            "Velociraptor",
            "venomous animals",
            "vertebrates",
            "viceroy butterfly",
            "vinegarroon",
            "viper",
            "Virginia opossum",
            "Vulcanodon",
            "vulture",
            "walkingstick",
            "wallaby",
            "walrus",
            "warthog",
            "wasp",
            "water moccasin",
            "water strider",
            "waterbug",
            "weasel",
            "Weddell seal",
            "weevil",
            "west highland white terrier",
            "western meadowlark",
            "western spotted owl",
            "whale",
            "whale shark",
            "whelk",
            "whip scorpion",
            "whippet",
            "white Bengal tiger",
            "white dove",
            "white pelican",
            "white rhinoceros",
            "white tiger",
            "white-breasted nuthatch",
            "white-spotted dolphin",
            "white-tailed deer",
            "wild cat",
            "wild dog",
            "wildebeest",
            "wolf",
            "wolverine",
            "wombat",
            "wood louse",
            "woodchuck",
            "woodland caribou",
            "woodpecker",
            "woolly bear caterpillar",
            "woolly mammoth",
            "woolly rhinoceros",
            "working dog",
            "worm",
            "wren",
            "Xenarthra (Edentata)",
            "xenops",
            "Xiaosaurus",
            "yak",
            "yellow mealworm",
            "yellow mongoose",
            "yellowjacket",
            "Yorkshire terrier",
            "zebra",
            "zebra bullhead shark",
            "zebra longwing butterfly",
            "zebra swallowtail butterfly",
            "zooplankton",
            "zorilla",
            "zorro"
    };

    String[] food = { //from https://www.enchantedlearning.com/
            "acorn squash",
            "alfalfa sprouts",
            "almond",
            "anchovy",
            "anise",
            "appetite",
            "appetizer",
            "apple",
            "apricot",
            "artichoke",
            "asparagus",
            "aspic",
            "ate",
            "avocado",
            "bacon",
            "bagel",
            "bake",
            "baked Alaska",
            "bamboo shoots",
            "banana",
            "barbecue",
            "barley",
            "basil",
            "batter",
            "beancurd",
            "beans",
            "beef",
            "beet",
            "bell pepper",
            "berry",
            "biscuit",
            "bitter",
            "black beans",
            "black tea",
            "black-eyed peas",
            "blackberry",
            "bland",
            "blood orange",
            "blueberry",
            "boil",
            "bowl",
            "boysenberry",
            "bran",
            "bread",
            "breadfruit",
            "breakfast",
            "brisket",
            "broccoli",
            "broil",
            "brown rice",
            "brownie",
            "brunch",
            "Brussels sprouts",
            "buckwheat",
            "buns",
            "burrito",
            "butter",
            "butter bean",
            "cake",
            "calorie",
            "candy",
            "candy apple",
            "cantaloupe",
            "capers",
            "caramel",
            "caramel apple",
            "carbohydrate",
            "carrot",
            "cashew",
            "cassava",
            "casserole",
            "cater",
            "cauliflower",
            "caviar",
            "cayenne pepper",
            "celery",
            "cereal",
            "chard",
            "cheddar",
            "cheese",
            "cheesecake",
            "chef",
            "cherry",
            "chew",
            "chick peas",
            "chicken",
            "chili",
            "chips",
            "chives",
            "chocolate",
            "chopsticks",
            "chow",
            "chutney",
            "cilantro",
            "cinnamon",
            "citron",
            "citrus",
            "clam",
            "cloves",
            "cobbler",
            "coconut",
            "cod",
            "coffee",
            "coleslaw",
            "collard greens",
            "comestibles",
            "cook",
            "cookbook",
            "cookie",
            "corn",
            "cornflakes",
            "cornmeal",
            "cottage cheese",
            "crab",
            "crackers",
            "cranberry",
            "cream",
            "cream cheese",
            "crepe",
            "crisp",
            "crunch",
            "crust",
            "cucumber",
            "cuisine",
            "cupboard",
            "cupcake",
            "curds",
            "currants",
            "curry",
            "custard",
            "daikon",
            "daily bread",
            "dairy",
            "dandelion greens",
            "Danish pastry",
            "dates",
            "dessert",
            "diet",
            "digest",
            "digestive system",
            "dill",
            "dine",
            "diner",
            "dinner",
            "dip",
            "dish",
            "dough",
            "doughnut",
            "dragonfruit",
            "dressing",
            "dried",
            "drink",
            "dry",
            "durian",
            "eat",
            "Edam cheese",
            "edible",
            "egg",
            "eggplant",
            "elderberry",
            "endive",
            "entree",
            "fast",
            "fat",
            "fava beans",
            "feast",
            "fed",
            "feed",
            "fennel",
            "fig",
            "fillet",
            "fire",
            "fish",
            "flan",
            "flax",
            "flour",
            "food",
            "food pyramid",
            "foodstuffs",
            "fork",
            "freezer",
            "French fries",
            "fried",
            "fritter",
            "frosting",
            "fruit",
            "fry",
            "garlic",
            "gastronomy",
            "gelatin",
            "ginger",
            "ginger ale",
            "gingerbread",
            "glasses",
            "Gouda cheese",
            "grain",
            "granola",
            "grape",
            "grapefruit",
            "grated",
            "gravy",
            "green bean",
            "green tea",
            "greens",
            "grub",
            "guacamole",
            "guava",
            "gyro",
            "halibut",
            "ham",
            "hamburger",
            "hash",
            "hazelnut",
            "herbs",
            "honey",
            "honeydew",
            "horseradish",
            "hot",
            "hot dog",
            "hot sauce",
            "hummus",
            "hunger",
            "hungry",
            "ice",
            "ice cream",
            "ice cream cone",
            "iceberg lettuce",
            "iced tea",
            "icing",
            "jackfruit",
            "jalapeño",
            "jam",
            "jelly",
            "jellybeans",
            "jicama",
            "jimmies",
            "Jordan almonds",
            "jug",
            "juice",
            "julienne",
            "junk food",
            "kale",
            "kebab",
            "ketchup",
            "kettle",
            "kettle corn",
            "kidney beans",
            "kitchen",
            "kiwi",
            "knife",
            "kohlrabi",
            "kumquat",
            "ladle",
            "lamb",
            "lard",
            "lasagna",
            "legumes",
            "lemon",
            "lemonade",
            "lentils",
            "lettuce",
            "licorice",
            "lima beans",
            "lime",
            "liver",
            "loaf",
            "lobster",
            "lollipop",
            "loquat",
            "lox",
            "lunch",
            "lunch box",
            "lunchmeat",
            "lychee",
            "macaroni",
            "macaroon",
            "main course",
            "maize",
            "mandarin orange",
            "mango",
            "maple syrup",
            "margarine",
            "marionberry",
            "marmalade",
            "marshmallow",
            "mashed potatoes",
            "mayonnaise",
            "meat",
            "meatball",
            "meatloaf",
            "melon",
            "menu",
            "meringue",
            "micronutrient",
            "milk",
            "milkshake",
            "millet",
            "mincemeat",
            "minerals",
            "mint",
            "mints",
            "mochi",
            "molasses",
            "mole sauce",
            "mozzarella",
            "muffin",
            "mug",
            "munch",
            "mushroom",
            "mussels",
            "mustard",
            "mustard greens",
            "mutton",
            "napkin",
            "nectar",
            "nectarine",
            "nibble",
            "noodles",
            "nosh",
            "nourish",
            "nourishment",
            "nut",
            "nutmeg",
            "nutrient",
            "nutrition",
            "nutritious",
            "oatmeal",
            "oats",
            "oil",
            "okra",
            "oleo",
            "olive",
            "omelet",
            "omnivore",
            "onion",
            "orange",
            "order",
            "oregano",
            "oven",
            "oyster",
            "pan",
            "pancake",
            "papaya",
            "parsley",
            "parsnip",
            "pasta",
            "pastry",
            "pate",
            "patty",
            "pattypan squash",
            "pea",
            "pea pod",
            "peach",
            "peanut",
            "peanut butter",
            "pear",
            "pecan",
            "pepper",
            "pepperoni",
            "persimmon",
            "pickle",
            "picnic",
            "pie",
            "pilaf",
            "pineapple",
            "pita bread",
            "pitcher",
            "pizza",
            "plate",
            "platter",
            "plum",
            "poached",
            "pomegranate",
            "pomelo",
            "pop",
            "popcorn",
            "popovers",
            "popsicle",
            "pork",
            "pork chops",
            "pot",
            "pot roast",
            "potato",
            "preserves",
            "pretzel",
            "prime rib",
            "protein",
            "provisions",
            "prune",
            "pudding",
            "pumpernickel",
            "pumpkin",
            "punch",
            "quiche",
            "quinoa",
            "radish",
            "raisin",
            "raspberry",
            "rations",
            "ravioli",
            "recipe",
            "refreshments",
            "refrigerator",
            "relish",
            "restaurant",
            "rhubarb",
            "ribs",
            "rice",
            "roast",
            "roll",
            "rolling pin",
            "romaine",
            "rosemary",
            "rye",
            "saffron",
            "sage",
            "salad",
            "salami",
            "salmon",
            "salsa",
            "salt",
            "sandwich",
            "sauce",
            "sauerkraut",
            "sausage",
            "savory",
            "scallops",
            "scrambled",
            "seaweed",
            "seeds",
            "sesame seed",
            "shallots",
            "sherbet",
            "shish kebab",
            "shrimp",
            "slaw",
            "slice",
            "smoked",
            "snack",
            "soda",
            "soda bread",
            "sole",
            "sorbet",
            "sorghum",
            "sorrel",
            "soup",
            "sour",
            "sour cream",
            "soy",
            "soy sauce",
            "soybeans",
            "spaghetti",
            "spareribs",
            "spatula",
            "spices",
            "spicy",
            "spinach",
            "split peas",
            "spoon",
            "spork",
            "sprinkles",
            "sprouts",
            "spuds",
            "squash",
            "squid",
            "steak",
            "stew",
            "stir-fry",
            "stomach",
            "stove",
            "straw",
            "strawberry",
            "string bean",
            "stringy",
            "strudel",
            "sub sandwich",
            "submarine sandwich",
            "succotash",
            "suet",
            "sugar",
            "summer squash",
            "sundae",
            "sunflower",
            "supper",
            "sushi",
            "sustenance",
            "sweet",
            "sweet potato",
            "Swiss chard",
            "syrup",
            "taco",
            "take-out",
            "tamale",
            "tangerine",
            "tapioca",
            "taro",
            "tarragon",
            "tart",
            "tea",
            "teapot",
            "teriyaki",
            "thyme",
            "toast",
            "toaster",
            "toffee",
            "tofu",
            "tomatillo",
            "tomato",
            "torte",
            "tortilla",
            "tuber",
            "tuna",
            "turkey",
            "turmeric",
            "turnip",
            "ugli fruit",
            "unleavened",
            "utensils",
            "vanilla",
            "veal",
            "vegetable",
            "venison",
            "vinegar",
            "vitamin",
            "wafer",
            "waffle",
            "walnut",
            "wasabi",
            "water",
            "water chestnut",
            "watercress",
            "watermelon",
            "wheat",
            "whey",
            "whipped cream",
            "wok",
            "yam",
            "yeast",
            "yogurt",
            "yolk",
            "zucchini"
    };
}