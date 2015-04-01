package core;

// Java Imports
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Other Imports
import configuration.GameServerConf;
import dataAccessLayer.DAO;
import dataAccessLayer.SpeciesDAO;
import dataAccessLayer.TileDAO;

import java.util.Collections;
import metadata.Constants;
import metadata.GameRequestTable;
import model.AnimalType;
import model.PlantType;
import model.Player;
import model.SpeciesType;
import model.Tile;
import utility.ConfFileParser;
import utility.ExpTable;
import utility.Log;
import utility.SpeciesComparator;

interface PlayerCallbackInterface {
    public void operation(Player p, Object param);
}

/**
 * The GameServer class serves as the main module that runs the server.
 * Incoming connection requests are established and redirected to be managed
 * by another class called the GameClient. Several specialized methods are also
 * stored here to perform other specific needs.
 */
public class GameServer {

    // Singleton Instance
    private static GameServer gameServer;
    // Server Variables
    private boolean isDone; // Server Loop Flag
    private GameServerConf configuration; // Stores server config. variables
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    
    // Season effects temporarily disabled by Yanxing
    //private ExecutorService seasonThread;
    
    // Reference Tables
    private final Map<String, GameClient> activeThreads = new HashMap<>(); // Session ID -> Client
    private final Map<Integer, Player> activePlayers = new HashMap<>(); // Player ID -> Player
    private final Map<Integer, AnimalType> animalTypes = new HashMap<>(); // Species ID -> Animal
    private final Map<Integer, PlantType> plantTypes = new HashMap<>(); // Species ID -> Plant
    //private Map<Integer, Tile> tiles = new HashMap<Integer, Tile>(); // TileID -> Tile
    // Other
    private EventHandler eventHandler;

    /**
     * Create the GameServer by setting up the request types and creating a
     * connection with the database.
     */
    public GameServer() {
        // Load configuration file
        configure();
        // Initialize tables for global use
        GameRequestTable.init(); // Contains request codes and classes
        ExpTable.init(); // Contains experience required per level

        // Initialize database connection
        if (DAO.getInstance() == null) {
            Log.println_e("Database Connection Failed!");
            System.exit(-1);
        }
        // Preload world-related objects
        initialize(); 
        
        //initialize season manager
        //seasonThread = Executors.newSingleThreadExecutor();
        
        //seasonThread.execute(SeasonManager.getInstance());
        // Thread Pool for Clients
        clientThreadPool = Executors.newCachedThreadPool();
        eventHandler = new EventHandler();
        // Update Badge Thresholds
        BadgeManager.setBadgeScores();

    }

    public static GameServer getInstance() {
        if (gameServer == null) {
            gameServer = new GameServer();
        }

        return gameServer;
    }

    /**
     * Load values from a configuration file.
     */
    public final void configure() {
        configuration = new GameServerConf();

        ConfFileParser confFileParser = new ConfFileParser("conf/gameServer.conf");
        configuration.setConfRecords(confFileParser.parse());
    }

    /**
     * Initialize the GameServer by loading a few things into memory.
     */
    public final void initialize() {
        setupSpeciesTypes();
        
        //set up tile table 
        //TileManager.getInstance().makeTile();

    }
    
    /**
     * Retrieve tiles from the database and load data into memory.

    public void setupTiles(){
        try {
            Log.println("Loading Tiles...");

            for (Tile tile : TileDAO.getTileList()) {
                tiles.put(tile.getTileId(), tile);
            }
        } catch (SQLException ex) {
            Log.println_e("Failed to retrieve Tiles.");
        }
    }
    public Tile getTile(int tile_id){
    	return tiles.get(tile_id);
    }*/
    
    /**
     * Retrieve species from the database and load data into memory.
     */
    public void setupSpeciesTypes() {
        try {
            Log.println("Loading Plant Types...");

            for (PlantType plant : SpeciesDAO.getPlants()) {
                plantTypes.put(plant.getID(), plant);
                plant.setAvgBiomass(500); // !!!
            }
        } catch (SQLException ex) {
            Log.println_e("Failed to retrieve Plant species.");
        }

        try {
            Log.println("Loading Animal Types...");

            for (AnimalType animal : SpeciesDAO.getAnimals()) {
                animalTypes.put(animal.getID(), animal);
                animal.setAvgBiomass(500); // !!!
            }
        } catch (SQLException ex) {
            Log.println_e("Failed to retrieve Animal species.");
        }

        Map<Integer, SpeciesType> speciesTypes = new HashMap<Integer, SpeciesType>();
        speciesTypes.putAll(plantTypes);
        speciesTypes.putAll(animalTypes);
    
        // Replace predator identifiers with actual instances
        Log.println("Resolving Predators...");
        for (SpeciesType species : speciesTypes.values()) {
            for (int predator_id : species.getPredatorIDs()) {
                if (speciesTypes.containsKey(predator_id)) {
                    species.resolvePredator(speciesTypes.get(predator_id));
                }
            }
        }

        // Replace prey identifiers with actual instances
        Log.println("Resolving Preys...");
        for (SpeciesType species : speciesTypes.values()) {
            for (int prey_id : species.getPreyIDs()) {
                if (speciesTypes.containsKey(prey_id)) {
                    species.resolvePrey(speciesTypes.get(prey_id));
                }
            }
        }
    }

    /**
     * Run the game server by waiting for incoming connection requests.
     * Establishes each connection and stores it into a GameClient to manage
     * incoming and outgoing activity.
     */
    private void run() {
        try {
            //seasonThread.execute(SeasonManager.getInstance());
            // Open a connection using the given port to accept incoming connections
            serverSocket = new ServerSocket(configuration.getPortNumber());
            Log.printf("Server has started on port: %d", serverSocket.getLocalPort());
            Log.println("Waiting for clients...");
            // Loop indefinitely to establish multiple connections
            while (!isDone) {
                try {
                    // Accept the incoming connection from client
                    Socket clientSocket = serverSocket.accept();
                    Log.printf("%s is connecting...", clientSocket.getInetAddress().getHostAddress());
                    // Create a runnable instance to represent a client that holds the client socket
                    String session_id = createUniqueID();
                    GameClient client = new GameClient(session_id, clientSocket);
                    // Keep track of the new client thread
                    addToActiveThreads(client);
                    // Initiate the client
                    clientThreadPool.submit(client);
                } catch (IOException e) {
                    Log.println_e(e.getMessage());
                }
            }
        } catch (IOException ex) {
            Log.println_e(ex.getMessage());
        }
    }

    public static String createUniqueID() {
        return UUID.randomUUID().toString();
    }

    public SpeciesType getSpecies(int species_id) {
        SpeciesType species = null;

        if (plantTypes.containsKey(species_id)) {
            species = plantTypes.get(species_id);
        } else if (animalTypes.containsKey(species_id)) {
            species = animalTypes.get(species_id);
        }

        return species;
    }

    public SpeciesType getSpecies(String name) {
        List<SpeciesType> speciesList = new ArrayList<SpeciesType>();
        speciesList.addAll(plantTypes.values());
        speciesList.addAll(animalTypes.values());
        
        int index = Collections.binarySearch(speciesList, new SpeciesType(name), SpeciesComparator.SpeciesNameComparator);

        return index < 0 ? null : speciesList.get(index);
    }

    public SpeciesType getSpeciesTypeByNodeID(int node_id) {
        return getSpeciesTypeByNodeList(new int[]{node_id});
    }

    public SpeciesType getSpeciesTypeByNodeList(int[] nodeList) {
        for (AnimalType animal : animalTypes.values()) {
            if (animal.equalsNodeList(nodeList)) {
                return animal;
            }
        }

        for (PlantType plant : plantTypes.values()) {
            if (plant.equalsNodeList(nodeList)) {
                return plant;
            }
        }

        return null;
    }

    public Map<String, GameClient> getActiveThreads() {
        return activeThreads;
    }

    /**
     * Get the GameClient thread for the player using the player ID.
     *
     * @param playerID holds the player ID
     * @return the GameClient thread
     */
    public GameClient getThreadByPlayerID(int playerID) {
        for (GameClient client : activeThreads.values()) {
            Player player = client.getPlayer();

            if (player != null && player.getID() == playerID) {
                return client;
            }
        }

        return null;
    }

    public void addToActiveThreads(GameClient client) {
        activeThreads.put(client.getID(), client);
    }
    
    public synchronized void traverseActivePlayers(Object param, PlayerCallbackInterface op) {
        //System.out.println(activePlayers.size());
        activePlayers.values().stream().forEach((p) -> {
            op.operation(p,param);
        });
    }

    public List<Player> getActivePlayers() {
        return new ArrayList<>(activePlayers.values());
    }

    public Player getActivePlayer(int player_id) {
        return activePlayers.get(player_id);
    }

    public void setActivePlayer(Player player) {
        activePlayers.put(player.getID(), player);
    }
    
    public void removeActivePlayer(int player_id) {
        activePlayers.remove(player_id);
    }
        
    public boolean isActive(int player_id) {
        return activePlayers.values().iterator().next().getID() == player_id;
    }

    /**
     * Delete a player's GameClient thread out of the activeThreads
     *
     * @param session_id              The id of the thread.
     */
    public void deletePlayerThreadOutOfActiveThreads(String session_id) {
        activeThreads.remove(session_id);
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * Initiates the Game Server by configuring and running it. Restarts
     * whenever it crashes.
     *
     * @param args contains additional launching parameters
     */
    public static void main(String[] args) {
        try {
            Log.printf("World of Balance Server v%s is starting...\n", Constants.CLIENT_VERSION);

            gameServer = new GameServer();
            gameServer.run();
        } catch (Exception ex) {
            Log.println_e("Server Crashed!");
            Log.println_e(ex.getMessage());

            try {
                Thread.sleep(10000);
                Log.println_e("Server is now restarting...");
                GameServer.main(args);
            } catch (InterruptedException ex1) {
                Log.println_e(ex1.getMessage());
            }
        }
    }
}
