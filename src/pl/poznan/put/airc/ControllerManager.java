package pl.poznan.put.airc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import pl.poznan.put.airc.lirc.ConfParser;
import pl.poznan.put.airc.lirc.Remote;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class ControllerManager {
	private static String ControllerManagerTag = "Airc.ControllerManager";
	private final Context context;

	private HashMap<UUID, Controller> controllers = null;
	private HashMap<String, Remote> remotes = null;

	private Controller activeController = null;
	private ToneSynthesizer synth = new ToneSynthesizer(
			AudioFormat.CHANNEL_OUT_STEREO, 19000);

	public ControllerManager(Context context) {
		this.context = context;

		this.discoverRemotes();
		{
			String msg = "Parsed remotes: ";
			for (String remote_name : this.remotes.keySet()) {
				msg += remote_name + ", ";
			}
			Log.i("AIRC", msg);
		}
		this.discoverControllers();

		// restore selected controller
		try {
			UUID uuid = UUID.fromString(this.context.getSharedPreferences("",
					Context.MODE_PRIVATE).getString("controller", null));
			if (uuid != null)
				this.activeController = this.controllers.get(uuid);
		} catch (IllegalArgumentException e) {
			Log.w(ControllerManagerTag, "controller preference was corrupted");
		} catch (NullPointerException e) {
			Log.w(ControllerManagerTag, "controller preference was corrupted");
		}
	}

	public void discoverControllers() {
		ContextWrapper cw = new ContextWrapper(this.context);

		this.controllers = new HashMap<UUID, Controller>();
		File controllersDir = cw.getDir("controllers", Context.MODE_PRIVATE);
		for (File file : controllersDir.listFiles()) {
			if (file.isFile() && file.canRead() && file.length() < 1000000L) {
				if (file.getName().toLowerCase().endsWith(".json")) {

					InputStreamReader isr;
					try {
						isr = new InputStreamReader(new FileInputStream(file),
								"UTF-8");

						String jString = "";

						char[] buf = new char[1024];
						int numRead = 0;
						while ((numRead = isr.read(buf)) != -1) {
							String readData = String.valueOf(buf, 0, numRead);
							jString += readData;
							buf = new char[1024];
						}

						JSONObject jObject = new JSONObject(jString);

						Controller controller = new Controller(jObject);
						this.controllers.put(controller.getUuid(), controller);

					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void discoverRemotes() {
		ContextWrapper cw = new ContextWrapper(this.context);

		this.remotes = new HashMap<String, Remote>();
		File lircconfsDir = cw.getDir("lircconfs", Context.MODE_PRIVATE);
		for (File file : lircconfsDir.listFiles()) {
			if (file.isFile() && file.canRead() && file.length() < 1000000L) {
				Log.d("AIRC", "parsing remotes conf file: " + file.getName());

				try {
					InputStreamReader isr = new InputStreamReader(
							new FileInputStream(file), "UTF-8");

					ConfParser parser = new ConfParser(isr);
					for (Remote remote : parser.Parse()) {
						this.remotes.put(remote.getName(), remote);
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void setActiveController(Controller activeController) {
		this.activeController = activeController;
		this.context.getSharedPreferences("", Context.MODE_PRIVATE).edit()
				.putString("controller", activeController.getUuid().toString())
				.commit();
	}

	public Controller getActiveController() {
		return this.activeController;
	}

	public HashMap<UUID, Controller> getControllers() {
		return this.controllers;
	}

	public HashMap<String, Remote> getRemotes() {
		return this.remotes;
	}

	public void click(int id) {
		if (this.activeController != null) {
			KeyMapping keymapping = this.activeController.getKey(id);
			if (keymapping != null) {
				Remote remote = this.getRemotes().get(keymapping.remote_name);
				final ArrayList<Integer> signalspacelist = remote
						.playButton(keymapping.remote_key);

				if (signalspacelist != null && signalspacelist.size() > 0) {

					AudioManager audioManager = (AudioManager) this.context
							.getSystemService(Context.AUDIO_SERVICE);
					audioManager
							.setStreamVolume(
									AudioManager.STREAM_MUSIC,
									audioManager
											.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 10 / 8,
									0);

					new Thread(new Runnable() {
						public void run() {
							synth.playTone(signalspacelist);
							Log.i("AIRC", "kkkk");
						}
					}).start();

					// AudioTrack audioTrack = synth.toneRawToAudioTrack(synth
					// .genToneRaw(19000, signalspacelist));
					// audioTrack.play();
				}
			}
		}

	}

	static public void save_remote_conf(Context context, String name,
			String conf_content) throws IOException {
		File lircconfsDir = context.getDir("lircconfs", Context.MODE_PRIVATE);
		name = name.replace("/", "_");

		BufferedWriter out = new BufferedWriter(new FileWriter(
				lircconfsDir.getAbsolutePath() + "/" + name));
		out.write(conf_content);
		out.close();
	}

	public void saveControllers() {
		File controllersDir = this.context.getDir("controllers",
				Context.MODE_PRIVATE);

		for (File file : controllersDir.listFiles()) {
			if (file.isFile() && file.canWrite()) {
				if (file.getName().toLowerCase().endsWith(".json")) {
					try {
						if (file.delete())
							Log.d("AIRC",
									"Controller file deleted: "
											+ file.getName());
						else
							Log.wtf("AIRC", "Controller file NOT deleted: "
									+ file.getName());
					} catch (SecurityException e) {
						Log.wtf("AIRC",
								"Controller file NOT deleted: "
										+ file.getName());
						e.printStackTrace();
					}
				}
			}
		}

		for (Controller controller : this.controllers.values()) {
			boolean success = false;
			try {
				String controllerJSON = controller.dump().toString();

				BufferedWriter out = new BufferedWriter(new FileWriter(
						controllersDir.getAbsolutePath() + "/"
								+ controller.getUuid().toString() + ".json"));
				out.write(controllerJSON);
				out.close();
				success = true;
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!success)
				Log.wtf("AIRC",
						"Controller dumping unsuccesful - "
								+ controller.getUuid().toString() + " - "
								+ controller.getName());
			else
				Log.d("AIRC",
						"Controller dumping succesful - "
								+ controller.getUuid().toString() + " - "
								+ controller.getName());
		}
	}

	void put(Controller controller) {
		this.controllers.put(controller.getUuid(), controller);
	}

	void remove(UUID controller_uuid) {
		this.controllers.remove(controller_uuid);
	}
}
