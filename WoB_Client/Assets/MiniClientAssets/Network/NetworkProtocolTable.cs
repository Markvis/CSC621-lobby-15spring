using UnityEngine;

using System;
using System.Collections.Generic;
using System.Reflection;

namespace MiniClient {
public class NetworkProtocolTable {

	private static Dictionary<short, Type> table = new Dictionary<short, Type>();

	private NetworkProtocolTable() {}

	public static void Init() {
		if (table.Count == 0) {
			Add (NetworkCode.CLIENT, "Client");
			Add (NetworkCode.MESSAGE, "Message");
			Add (NetworkCode.CHANGE_NAME, "ChangeName");
			Add (NetworkCode.REQUEST_START, "RequestStart");
		}
	}
	
	public static void Add(short protocol_id, string name) {
		Type type = Type.GetType("MiniClient." + name + "Protocol");

		if (type != null) {
			if (!table.ContainsKey(protocol_id)) {
				table.Add(protocol_id, type);
			} else {
				Debug.LogError("Protocol ID " + protocol_id + " already exists! Ignored " + name);
			}
		} else {
			Debug.LogError(name + " not found");
		}
	}
	
	public static Type Get(short protocol_id) {
		Type type = null;
		
		if (table.ContainsKey(protocol_id)) {
			type = table[protocol_id];
		} else {
			Debug.LogError("Protocol [" + protocol_id + "] Not Found");
		}
		
		return type;
	}
}
}