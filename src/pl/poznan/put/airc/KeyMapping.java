package pl.poznan.put.airc;

import org.json.JSONException;
import org.json.JSONObject;

public class KeyMapping {
	public int btn_mapping;
	public String btn_text;
	
	public String remote_name;
	public String remote_key;
	
	public KeyMapping(int btn_mapping, String btn_text, String remote_name, String remote_key){
		this.btn_mapping = btn_mapping;
		this.btn_text = btn_text;
		this.remote_name = remote_name;
		this.remote_key = remote_key;
	}
	
	public KeyMapping(JSONObject keydump) throws JSONException {
		this.btn_mapping = keydump.getInt("btn_mapping");
		this.btn_text = keydump.getString("btn_text");
		this.remote_name = keydump.getString("remote_name");
		this.remote_key = keydump.getString("remote_key");
	}
	
	public JSONObject dump() throws JSONException {
		JSONObject dumpped = new JSONObject();
		dumpped.put("btn_mapping", this.btn_mapping);
		dumpped.put("btn_text", this.btn_text);
		dumpped.put("remote_name", this.remote_name);
		dumpped.put("remote_key", this.remote_key);

		return dumpped;	
	}
}