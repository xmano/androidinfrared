package pl.poznan.put.airc.lirc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import android.util.Log;

/**
 * 
 * As reference http://winlirc.sourceforge.net/technicaldetails.html was used
 */
public class Remote {
	protected String name;// <remote name> The unique name assigned to the
							// remote control (may not contain whitespace).

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected ArrayList<String> flags; // flags <flag1><|flag2>. . . etc. Flags are
								// special text strings which describe various
								// properties of the remote, and determine the
								// meaning of some of the following fields.
								// Multiple flags are allowed if separated by
								// the pipe(|) symbol.
	protected HashMap<String, ArrayList<Integer>> num_attr = new HashMap<String, ArrayList<Integer>>();

	protected HashMap<String, ArrayList<Integer>> raw_codes = new HashMap<String, ArrayList<Integer>>();
	protected HashMap<String, Integer> codes = new HashMap<String, Integer>();

	boolean toggle_bit_state = false; // initial state shouldn't matter anyway

	Remote() {
		this.name = "Unknown_" + UUID.randomUUID().toString();

		this.num_attr.put("duty_cycle",
				new ArrayList<Integer>(Arrays.asList(50))); // add default value
	}
	
	public ArrayList<String> getButtonsNames() {
		ArrayList<String> btnnames = new ArrayList<String>();
		
		//well, there can be duplicated names, screw it (if sanity check wasn't used)
		
		for(String btnname : this.codes.keySet())
		{
			btnnames.add(btnname);
		}
		
		for(String btnname : this.raw_codes.keySet())
		{
			btnnames.add(btnname);
		}
		
		return btnnames;
	}

	/**
	 * Checking if `Remote' data makes any sense
	 * 
	 * @return true if everything looks OK
	 */
	boolean sanityCheck() {
		for (Entry<String, Integer> entry : ConfParser.num_attr_supported
				.entrySet()) {
			if (this.num_attr.containsKey(entry.getKey())) {
				if (entry.getValue() != this.num_attr.get(entry.getKey())
						.size())
					return false;
			}
		}

		for (String key : this.codes.keySet()) {
			if (this.raw_codes.containsKey(key))
				return false;
		}

		return true;
	}

	/**
	 * Render button code to the raw equivalent
	 * 
	 * @param code
	 *            code to be converted
	 * @return raw equivalent of `code'
	 */
	protected ArrayList<Integer> codeToRaw(int code, int bits) {
		ArrayList<Integer> raw = new ArrayList<Integer>();

		if (this.flags.contains("REVERSE")) {
			for (int i = 0; i < bits; i++) {
				if (((1 << i) & code) == 0) {
					raw.addAll(this.num_attr.get("zero"));
				} else {
					raw.addAll(this.num_attr.get("one"));
				}
			}
		}
		else
		{
			for (int i = bits; i > 0; i--) {
				if (((1 << i) & code) == 0) {
					raw.addAll(this.num_attr.get("zero"));
				} else {
					raw.addAll(this.num_attr.get("one"));
				}
			}
		}

		return raw;
	}

	private ArrayList<Integer> playRaw(ArrayList<Integer> raw_code) {
		ArrayList<Integer> raw_pulse_space = new ArrayList<Integer>();

		if (this.num_attr.containsKey("header")) {
			raw_pulse_space.addAll(this.num_attr.get("header"));
		}

		if (this.num_attr.containsKey("plead")) {
			raw_pulse_space.add(this.num_attr.get("plead").get(0));
			raw_pulse_space.add(0);// TODO check it! pulse without space after?
		}

		if (this.num_attr.containsKey("pre_data")
				&& this.num_attr.containsKey("pre_data_bits")) {
			raw_pulse_space.addAll(this.codeToRaw(this.num_attr.get("pre_data")
					.get(0), this.num_attr.get("pre_data_bits").get(0)));
		}

		if (this.num_attr.containsKey("pre")) {
			raw_pulse_space.addAll(this.num_attr.get("pre"));
		}

		raw_pulse_space.addAll(raw_code);

		if (this.num_attr.containsKey("post")) {
			raw_pulse_space.addAll(this.num_attr.get("post"));
		}

		if (this.num_attr.containsKey("post_data")
				&& this.num_attr.containsKey("post_data_bits")) {
			raw_pulse_space.addAll(this.codeToRaw(this.num_attr
					.get("post_data").get(0),
					this.num_attr.get("post_data_bits").get(0)));
		}

		if (this.num_attr.containsKey("ptrail")) {
			raw_pulse_space.add(this.num_attr.get("ptrail").get(0));
			raw_pulse_space.add(0);// TODO check it! pulse without space after?
		}

		// not sure about this placement
		if (this.num_attr.containsKey("repeat_gap")) {
			raw_pulse_space.add(0);
			raw_pulse_space.add(this.num_attr.get("repeat_gap").get(0));
		}

		// not sure about this placement
		// TODO handle CONST_LENGTH flag
		if (this.num_attr.containsKey("gap")) {
			raw_pulse_space.add(0);
			raw_pulse_space.add(this.num_attr.get("gap").get(0));
		}

		if (this.num_attr.containsKey("foot")) {
			raw_pulse_space.addAll(this.num_attr.get("foot"));
		}

		return raw_pulse_space;
	}

	public ArrayList<Integer> playButton(String btn_name) {
		ArrayList<Integer> raw;
		if (this.raw_codes.containsKey(btn_name))
			raw = this.raw_codes.get(btn_name);
		else
			raw = this.codeToRaw(this.codes.get(btn_name),
					this.num_attr.get("bits").get(0));

		return this.playRaw(raw);
	}

}
