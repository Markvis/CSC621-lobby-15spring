package networking.request;

// Java Imports
import java.io.IOException;
import java.util.List;

// Other Imports
import core.GameClient;
import core.GameServer;
import core.Lobby;
import core.LobbyManager;
import core.WorldManager;
import dataAccessLayer.AvatarDAO;
import dataAccessLayer.PlayerDAO;
import dataAccessLayer.WorldDAO;
import metadata.Constants;
import model.Avatar;
import model.Player;
import model.World;
import networking.response.ResponseAvatarList;
import networking.response.ResponseLogin;
import networking.response.ResponseWorld;
import networking.response.ResponseWorldMenuAction;
import utility.DataReader;
import utility.Log;

/**
 * The RequestLogin class authenticates the user information to log in. Other
 * tasks as part of the login process lies here as well.
 */
public class RequestLogin extends GameRequest {

    // Data
    private String version;
    private String user_id;
    private String password;
    // Responses
    private ResponseLogin responseLogin;

    public RequestLogin() {
        responses.add(responseLogin = new ResponseLogin());
    }

    @Override
    public void parse() throws IOException {
        version = DataReader.readString(dataInput).trim();
        user_id = DataReader.readString(dataInput).trim();
        password = DataReader.readString(dataInput).trim();
    }

    @Override
    public void doBusiness() throws Exception {
        Log.printf("User '%s' is connecting...", user_id);

        Player player = null;
        // Checks if the connecting client meets the minimum version required
        if (version.compareTo(Constants.CLIENT_VERSION) >= 0) {
            // Ensure the user ID is in the correct format
            if (!user_id.isEmpty() && !password.isEmpty()) {
                // Attempt to retrieve the account
                player = PlayerDAO.getAccount(user_id, password);
            }

            if (player == null) {
                responseLogin.setStatus((short) 1); // User info is incorrect
                Log.printf("User '%s' has failed to log in.", user_id);
            } else {
                // Prevent consecutive login attempts
                if (client.getPlayer() == null || player.getID() != client.getPlayer().getID()) {
                    GameClient thread = GameServer.getInstance().getThreadByPlayerID(player.getID());
                    // If account is already in use, remove and disconnect the client
                    if (thread != null) {
                        responseLogin.setStatus((short) 2); // Account is in use
                        thread.removePlayerData();
                        thread.newSession();
                        Log.printf("User '%s' account is already in use.", user_id);
                    } else {
                        // Continue with the login process
                        PlayerDAO.updateLogin(player.getID(), client.getIP());
                        GameServer.getInstance().setActivePlayer(player);
                        player.setClient(client);
                        player.setLastSaved(System.currentTimeMillis());
                        player.startSaveTimer();
                        // Pass Player reference into thread
                        client.setPlayer(player);
                        // Set response information
                        responseLogin.setStatus((short) 0); // Login is a success
                        responseLogin.setPlayer(player);
                        

                        Log.printf("User '%s' has successfully logged in.", player.getUsername());
                        
                        //author: Lobby Team
                        //send world information
                        World world = WorldDAO.getPlayerWorlds(player.getID()).get(0);
                        world = WorldManager.getInstance().createExistingWorld(world.getID());
                        ResponseWorld responseWorld = new ResponseWorld();
                        responseWorld.setStatus((short) 0);
                        responseWorld.setWorld(world);
                        responses.add(responseWorld);
                        
                        //send avatar information
                        List<Avatar> avatarList = AvatarDAO.getAvatars(player.getID());
                        ResponseAvatarList responseAvatarList = new ResponseAvatarList();
                        responseAvatarList.setAvatarList(avatarList);
                        responses.add(responseAvatarList);
                        //assign lobby
                        WorldManager.getInstance().createLobby(world,client);

                    }
                } else {
                    responseLogin.setStatus((short) 4); // Consecutive logins
                }
            }
        } else {
            responseLogin.setStatus((short) 3); // Client version not compatible
            Log.printf("User '%s' has failed to log in. (v%s)", player.getUsername(), version);
        }
    }
    


}
