using UnityEngine;

using System.Collections;
using System.Collections.Generic;

public class GameState : MonoBehaviour {

	public static AvatarData avatar;
	public static WorldData world;
	private int month;
	public Dictionary<int, Species> speciesList { get; set; }
	
	// Use this for initialization
	void Awake () {
		speciesList = new Dictionary<int, Species>();

		GameObject.Find("MainObject").GetComponent<MessageQueue>().AddCallback(Constants.SMSG_CREATE_ENV, ResponseCreateEnv);
		GameObject.Find("MainObject").GetComponent<MessageQueue>().AddCallback(Constants.SMSG_SPECIES_CREATE, ResponseSpeciesCreate);
	}
	
	// Update is called once per frame
	void Update () {

	}
	
	public Species GetSpeciesGroup(int group_id) {
		return speciesList.ContainsKey(group_id) ? speciesList[group_id] : null;
	}

	public void CreateSpecies(int species_id, string type, string organism_type, int biomass) {
		if (speciesList.ContainsKey(species_id)) {
			UpdateSpecies(species_id, biomass);
		} else {
			Species species = gameObject.AddComponent<Species>();
			species.species_id = species_id;
			species.name = type;
			species.organism_type = organism_type;
			species.biomass = biomass;
	
			speciesList.Add(species_id, species);
		}
	}
	
	public void UpdateSpecies(int species_id, int size) {
		Species species = speciesList[species_id];
		species.UpdateSize(size);
	}
	
	public void ResponseCreateEnv(ExtendedEventArgs eventArgs) {
		ResponseCreateEnvEventArgs args = eventArgs as ResponseCreateEnvEventArgs;

		//GetComponent<EnvironmentScore>().SetScore(args.score);
	}
	
	public void ResponseSpeciesCreate(ExtendedEventArgs eventArgs) {
		ResponseSpeciesCreateEventArgs args = eventArgs as ResponseSpeciesCreateEventArgs;

		ConnectionManager cManager = GameObject.Find("MainObject").GetComponent<ConnectionManager>();

		//request to start world, start first simulation
		if (cManager) {
			RequestReady request = new RequestReady();
			request.Send(true);
			
			cManager.Send(request);
		}
		//start game loop
		//GameObject.Find("MainObject").AddComponent("Clock");
		//GameObject.Find("MainObject").AddComponent("GameLoop");

		
		/*if (args.species_id < 1000) {
			CreateSpecies(args.species_id, SpeciesTable.speciesList[args.species_id].name, "Animal", 500);
		} else {
			CreateSpecies(args.species_id, SpeciesTable.speciesList[args.species_id].name, "Plant", 500);
		}

		UpdateSpecies(args.species_id, args.biomass * 2);*/
	}
}
