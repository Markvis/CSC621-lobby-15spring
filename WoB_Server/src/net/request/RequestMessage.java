package net.request;

// Java Imports
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

// Other Imports
import db.LogDAO;
import net.response.ResponseMessage;
import util.DataReader;
import util.NetworkFunctions;
import model.Player;
import core.GameServer;

/**
 * The RequestMessage class handles all incoming chat messages and redirect those
 * messages, if needed, to other users.
 * 
 * types:
 * 0 - regular message
 * 1 - server message (?)
 * 2 - private message
 * 
 * status:
 * 0 - OK
 * 1 - whipser failed
 */
public class RequestMessage extends GameRequest {

    private short type;
    private String message;
    private String recipient;

    @Override
    public void parse(DataInputStream dataInput) throws IOException {
        type = DataReader.readShort(dataInput);
        message = DataReader.readString(dataInput).trim();
        recipient = DataReader.readString(dataInput);

        if (message.isEmpty()) {
            throw new IOException();
        }
    }

    @Override
    public void process() throws Exception {
        LogDAO.createMessage(client.getAccount().getID(), message);

        ResponseMessage response = new ResponseMessage();
        response.setMessage(message);
        response.setName(client.getPlayer().getName());
        response.setType(type);
        
        if (type == 0) {
            NetworkFunctions.sendToGlobal(response);
        } else if (type == 2) { //private message - get online players, search for recipient, 
                                //and either send message to recipient or error back to sender
            List<Player> players = GameServer.getInstance().getActivePlayers();
            int playerID = 0;
            boolean found = false;
            
            for (int i=0; i<players.size(); i++) {
                if (players.get(i).getName().equals(recipient)) {
                    playerID = players.get(i).getID();
                    found = true;
                    break;
                }
            }
            
            if (found) {
                response.setStatus((short)0);
                NetworkFunctions.sendToPlayer(response, playerID);
            } else {
                response.setStatus((short)1);
                client.add(response);
            }
        }
    }
}
