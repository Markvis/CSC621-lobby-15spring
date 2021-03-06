/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.request.clashgame;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import core.GameServer;
import db.clashgame.DefenseConfigDAO;
import model.clashgame.DefenseConfig;
import net.request.GameRequest;
import net.response.clashgame.ResponseClashDefenseSetup;
import util.DataReader;
import util.Vector2;

/**
 * Network request called when user wants to create or update
 * his or her defense configuration for the Clash of Species game
 * @author lev
 */
public class RequestClashDefenseSetup extends GameRequest {

    /**
     * Stores the terrain ID for the new defense setup
     */
    private String setupTerrain;

    /**
     * Stores the species and positions for the new defense setup
     */
    private HashMap<Integer, Vector2<Float>> configMap
             = new HashMap<Integer, Vector2<Float>>();

    /**
     * Fills the instance variables with data received over the
     * network
     * @param dataInput the input stream containg data sent by the
     *                  client
     * @throws IOException
     */
    @Override
    public void parse(DataInputStream dataInput) throws IOException {
        setupTerrain = DataReader.readString(dataInput);
        int defenseSpeciesCount = DataReader.readInt(dataInput);
        for(int i = 0; i < defenseSpeciesCount; i++){
            int speciesId = DataReader.readInt(dataInput);
            float x = DataReader.readFloat(dataInput);
            float y = DataReader.readFloat(dataInput);
            
            configMap.put(speciesId, new Vector2(x, y));
        }
    }

    /**
     * Checks the validity of the sent defense configuration
     * If configuration was valid, saves the config in the
     * database
     * Sends the validity inside the response back to the client
     * @throws Exception
     */
    @Override
    public void process() throws Exception {
        boolean valid = configMap.size() == 5; //more checks in the future

        ResponseClashDefenseSetup response = new ResponseClashDefenseSetup();
        response.setValidSetup(valid);

        if (valid) {
            DefenseConfig config = new DefenseConfig();
            config.createdAt = new Date();
            config.playerId = client.getPlayer().getID();
            config.terrain = setupTerrain;
            config.layout = configMap;
            DefenseConfigDAO.create(config);
        }

        client.add(response);
    }
    
}
