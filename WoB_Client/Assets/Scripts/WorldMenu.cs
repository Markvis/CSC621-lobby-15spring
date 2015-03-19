using UnityEngine;

using System.Collections;
using System.Collections.Generic;

public class WorldMenu : MonoBehaviour {
	
	private GameObject mainObject;
	// Window Properties
	private float width = 300;
	private float height = 100;
	// Other
	private Rect windowRect;
	private Rect avatarRect;
	private Texture avatar;
	private Rect[] buttonRectList;
	private GameObject messageBox;
	public string name { get; set; }
	public short level { get; set; }
	public int coins { get; set; }
	public string last_logout { get; set; }
	private Dictionary<int, AvatarData> avatarList = new Dictionary<int, AvatarData>();
	private Dictionary<int, WorldData> worldList = new Dictionary<int, WorldData>();
	private Vector2 scrollViewVector;
	private string innerText = "";
	private AvatarData avatarSelected;
	private WorldData worldSelected;
	
	void Awake() {
		mainObject = GameObject.Find("MainObject");
		buttonRectList = new Rect[3];
	}
	
	// Use this for initialization
	void Start() {
		windowRect = new Rect (0, 0, width, height);
		windowRect.x = (Screen.width - windowRect.width) / 2;
		windowRect.y = (Screen.height - windowRect.height) / 2;
		
		SetAvatarImage("");
		
		ConnectionManager cManager = mainObject.GetComponent<ConnectionManager>();

		if (cManager) {
			{
				RequestAvatarList request = RequestAvatarList(Constants.USER_ID);
				cManager.Send(request);
				
				mainObject.GetComponent<MessageQueue>().AddCallback(Constants.SMSG_AVATAR_LIST, ResponseAvatarList);
			}
			
			{
				RequestWorldList request = RequestWorldList(Constants.USER_ID);
				cManager.Send(request);

				mainObject.GetComponent<MessageQueue>().AddCallback(Constants.SMSG_WORLD_LIST, ResponseWorldList);
			}
		}

		mainObject.GetComponent<MessageQueue>().AddCallback(Constants.SMSG_WORLD_MENU_ACTION, ResponseWorldMenuAction);
		mainObject.GetComponent<MessageQueue>().AddCallback(Constants.SMSG_SPECIES_ACTION, ResponseSpeciesAction);
	}
	
	// Update is called once per frame
	void Update() {
	}
	
	void OnGUI() {
		windowRect = GUILayout.Window((int) Constants.GUI_ID.World_Menu, windowRect, MakeWindow, "World Menu");
		
		windowRect.x = (Screen.width - windowRect.width) / 2;
		windowRect.y = (Screen.height - windowRect.height) / 2;
	}
	
	void MakeWindow(int id) {
		avatarRect = new Rect(20, 30, 80, 80);
		GUI.Box(avatarRect, "");
//		GUI.DrawTexture(avatarRect, avatar);
		
		List<WorldData> wListSorted = new List<WorldData>(worldList.Values);
		
		Rect wListBox = new Rect(20, 120, 180, 30);
		GUI.Box(wListBox, "");

		scrollViewVector = GUI.BeginScrollView(wListBox, scrollViewVector, new Rect(0, 0, wListBox.width - 16, wListBox.height * wListSorted.Count), false, true);
			for (int i = 0; i < wListSorted.Count; i++) {
				if (wListSorted[i] == worldSelected) {
					GUI.backgroundColor = Color.black;
				}

				if (GUI.Button(new Rect(0, i * 30, wListBox.width - 16, wListBox.height), wListSorted[i].name)) {
					worldSelected = wListSorted[i];
				}

				GUI.backgroundColor = Color.white;
			}
		GUI.EndScrollView();
		
		GUIStyle style = new GUIStyle();

		if (avatarSelected != null) {
			style.alignment = TextAnchor.UpperLeft;
			style.normal.textColor = Color.white;
	
			GUI.Label(new Rect(115, 40, 100, 100), name, style);
			GUI.Label(new Rect(115, 60, 100, 100), "Level " + avatarSelected.level, style);
			
			style = new GUIStyle();
			style.alignment = TextAnchor.UpperRight;
			style.normal.textColor = Color.white;
			
			GUI.Label(new Rect(170, 60, 100, 100), avatarSelected.coins + " Coins", style);
			
			style = new GUIStyle();
			style.alignment = TextAnchor.UpperCenter;
			style.normal.textColor = Color.white;
			
			GUI.Label(new Rect(140, 80, 100, 100), "Last Played on " + last_logout.Split(' ')[0], style);
		}

		if (worldSelected != null) {
			style = new GUIStyle();
			style.alignment = TextAnchor.UpperLeft;
			style.normal.textColor = Color.white;
			
			GUI.Label(new Rect(20, 160, 100, 100), "Type " + (worldSelected.type + 1), style);
			GUI.Label(new Rect(20, 180, 100, 100), "Played " + (worldSelected.play_time / 3600).ToString() + ":" + (worldSelected.play_time % 3600 / 60).ToString().PadLeft(2, '0'), style);
	
			style = new GUIStyle();
			style.alignment = TextAnchor.UpperRight;
			style.normal.textColor = Color.white;
			
			GUI.Label(new Rect(100, 160, 100, 100), "Credits: " + worldSelected.credits, style);
			GUI.Label(new Rect(100, 180, 100, 100), "Score: " + worldSelected.score, style);
		}

		GUILayout.Space(190);

		for (int i = 0; i < 3; i++) {
			buttonRectList[i] = new Rect(windowRect.width - 85, 120 + i * 30, 75, 25);
		}
		
		if (GUI.Button(buttonRectList[0], "Join")) {
			Join();
		}
		
		if (GUI.Button(buttonRectList[1], "Create")) {
			Create();
		}
		
		if (GUI.Button(buttonRectList[2], "Delete")) {
			Delete();
		}
	}
	
	public void SetAvatarImage(string avatar_id) {
	}
	
	public void Create() {
		ConnectionManager cManager = mainObject.GetComponent<ConnectionManager>();
		
		if (cManager) {
			RequestWorldMenuAction request = new RequestWorldMenuAction();
			request.CreateAction(0, "", 1, "Savanna", 1, "");
			
			cManager.Send(request);
//			messageBox = mainObject.GetComponent<Main>().CreateMessageBox("Creating World...");

			// Get Default Species
			RequestSpeciesAction sRequest = new RequestSpeciesAction();
			sRequest.Send(0, 0);
			cManager.Send(sRequest);
		}
	}
	
	public void Join() {
		ConnectionManager cManager = mainObject.GetComponent<ConnectionManager>();
		
		if (cManager) {
			RequestWorldMenuAction request = new RequestWorldMenuAction();
			request.JoinAction(worldSelected.world_id);
			
			cManager.Send(request);
//			messageBox = mainObject.GetComponent<Main>().CreateMessageBox("Joining World...");
		}
	}

	public void Delete() {
		ConnectionManager cManager = mainObject.GetComponent<ConnectionManager>();
		
		if (cManager) {
			RequestWorldMenuAction request = new RequestWorldMenuAction();
			request.DeleteAction(worldSelected.world_id);
			
			cManager.Send(request);
//			messageBox = mainObject.GetComponent<Main>().CreateMessageBox("Deleting World...");
		}
	}

	public void SwitchToLogin() {
		Destroy(this);
		GameObject.Find("LoginObject").GetComponent<Login>().Show();
	}
	
	public RequestAvatarList RequestAvatarList(int user_id) {
		RequestAvatarList request = new RequestAvatarList();
		request.Send (user_id);
		
		return request;
	}
	
	public void ResponseAvatarList(ExtendedEventArgs eventArgs) {
		ResponseAvatarListEventArgs args = eventArgs as ResponseAvatarListEventArgs;
		
		if (args.status == 0) {
			avatarList = args.avatarList;
		}

		if (avatarList.Count > 0) {
			avatarSelected = new List<AvatarData>(avatarList.Values)[0];
			GameState.avatar = avatarSelected;
		}
	}
	
	public RequestWorldList RequestWorldList(int user_id) {
		RequestWorldList request = new RequestWorldList();
		request.Send(user_id);
		
		return request;
	}

	public void ResponseWorldList(ExtendedEventArgs eventArgs) {
		ResponseWorldListEventArgs args = eventArgs as ResponseWorldListEventArgs;
		
		if (args.status == 0) {
			worldList = args.worldList;
			
			if (worldList.Count > 0) {
				worldSelected = new List<WorldData>(worldList.Values)[0];
			}
		}
	}

	public void ResponseWorldMenuAction(ExtendedEventArgs eventArgs) {
		ResponseWorldMenuActionEventArgs args = eventArgs as ResponseWorldMenuActionEventArgs;

		switch (args.action) {
			case 1: // Create
				if (args.status == 0) {
					WorldData world = new WorldData(args.world_id);
					world.name = args.name;
					world.credits = args.credits;
					world.month = args.month;
		
					worldList.Add(args.world_id, world);
				}
				break;
			case 2: // Join
				if (args.status == 0) {
					WorldData world = new WorldData(args.world_id);
					world.name = args.name;
					world.credits = args.credits;
					world.month = args.month;
	
					GameState.world = world;
					Application.LoadLevel("World");//World
				}
				break;
			case 3: // Delete
				worldList.Remove(args.world_id);

				if (worldList.Count > 0) {
					worldSelected = new List<WorldData>(worldList.Values)[0];
				}
				break;
			default:
				break;
		}
	}
	
	public void ResponseSpeciesAction(ExtendedEventArgs eventArgs) {
		ResponseSpeciesActionEventArgs args = eventArgs as ResponseSpeciesActionEventArgs;

		if (args.action == 0) {
			Dictionary<int, int> speciesList = new Dictionary<int, int>();

			foreach (string item in args.selectionList.Split(',')) {
				string[] pair = item.Split(':');
				int species_id = int.Parse(pair[0]);
				int biomass = int.Parse(pair[1]);

				speciesList.Add(species_id, biomass);
				Debug.Log(species_id + " " + biomass);
			}

			ConnectionManager cManager = mainObject.GetComponent<ConnectionManager>();
			
			if (cManager) {
				RequestSpeciesAction request = new RequestSpeciesAction();
				request.Send(1, speciesList);

				cManager.Send(request);
			}
		}
	}
}
