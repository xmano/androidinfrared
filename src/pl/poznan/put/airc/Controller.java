package pl.poznan.put.airc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class Controller {
	private String name;// Human readable name of the controller
	private HashMap<Integer, KeyMapping> keymap = new HashMap<Integer, KeyMapping>();

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	private final UUID uuid;

	public UUID getUuid() {
		return uuid;
	}

	public Controller(String name) {
		this.name = name;
		this.uuid = UUID.randomUUID();
	}

	public Controller(JSONObject jObject) throws JSONException {
		this.name = jObject.getString("name");
		this.uuid = UUID.fromString(jObject.getString("uuid"));
		JSONObject km = jObject.getJSONObject("keymap");
		@SuppressWarnings("unchecked")
		Iterator<String> it = km.keys();
		while (it.hasNext()) {
			String key = it.next();
			KeyMapping keymapping = new KeyMapping((JSONObject) km.get(key));
			this.keymap.put(Integer.valueOf(key), keymapping);
		}
	}

	public JSONObject dump() throws JSONException {
		JSONObject dumpped = new JSONObject();

		dumpped.put("name", this.name);
		dumpped.put("uuid", this.uuid.toString());

		JSONObject km = new JSONObject();
		Iterator<Entry<Integer, KeyMapping>> it = this.keymap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Entry<Integer, KeyMapping> pair = it.next();
			km.put(pair.getKey().toString(), pair.getValue().dump());
		}
		dumpped.put("keymap", km);

		return dumpped;
	}

	public void setKey(KeyMapping keymapping) {
		this.keymap.put(keymapping.btn_mapping, keymapping);
	}

	public KeyMapping getKey(int id) {
		return this.keymap.get(id);
	}

}
