package pl.poznan.put.airc;

import java.util.Iterator;
import java.util.Map.Entry;

import pl.poznan.put.airc.lirc.Remote;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class AircActivity extends Activity implements
		AdapterView.OnItemSelectedListener{
	private boolean editBtns = false;
	private ControllerManager controllermanager = null;
	private ControllersSpinnerAdapter controllersAdapter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TableLayout layout = (TableLayout) findViewById(R.id.BtnTable);
		if (layout != null) {
			for (int i = 0; i < layout.getChildCount(); i++) {
				TableRow tr = (TableRow) layout.getChildAt(i);
				for (int j = 0; j < tr.getChildCount(); j++) {
					View v = tr.getChildAt(j);
					registerForContextMenu(v);
				}
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (controllermanager == null)
			controllermanager = new ControllerManager(getBaseContext());

		Spinner spinner = (Spinner) findViewById(R.id.ControllerSpinner);

		this.controllersAdapter = new ControllersSpinnerAdapter(this);
		// controllersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(this.controllersAdapter);
	}

	@Override
	public void onResume() {
		super.onResume();

		this.listControllers();
		this.renameButtons(false);
	}

	@Override
	public void onStop() {
		this.controllermanager.saveControllers();

		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean("editBtns", this.editBtns);

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		this.editBtns = savedInstanceState.getBoolean("editBtns");
	}

	public void onRemoteClick(View view) {

		if (this.editBtns) {

		} else {
			this.controllermanager.click(view.getId());
		}
	}
	
	public void onControllerDelClick(View view) {
		final Spinner spinner = (Spinner) findViewById(R.id.ControllerSpinner);
		final ControllersSpinnerAdapter cadapter = (ControllersSpinnerAdapter) spinner.getAdapter();
		final Controller controller = (Controller) spinner.getSelectedItem();
		if (controller != null) {
			final boolean editBtns_ = editBtns;
			new AlertDialog.Builder(this)
					.setTitle("Delete controller")
					.setMessage("Delete this controller?")
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									controllermanager.remove(controller
											.getUuid());
									if (controllermanager.getControllers()
											.size() > 0)
										controllermanager
												.setActiveController((Controller) cadapter
														.getItem(0));
									listControllers();
									spinner.setSelection(0);
									renameButtons(editBtns_);
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.btn_contextmenu, menu);

		if (this.controllermanager.getRemotes() != null && this.editBtns) {
			Iterator<Entry<String, Remote>> it = this.controllermanager
					.getRemotes().entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Remote> pair = (Entry<String, Remote>) it.next();
				String remote_name = pair.getKey();
				SubMenu submenu = menu.addSubMenu(remote_name);
				Remote remote = pair.getValue();

				for (String btnname : remote.getButtonsNames()) {
					MenuItem menuitem = submenu.add(btnname);
					final int f_id = v.getId();

					final String f_remote_name = remote_name;
					final String f_btnname = btnname;
					menuitem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						public boolean onMenuItemClick(MenuItem item) {
							Controller controller = controllermanager
									.getActiveController();
							if (controller != null)
								controller.setKey(new KeyMapping(f_id,
										f_btnname, f_remote_name, f_btnname));

							renameButtons(true);
							return true;
						}
					});
				}
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optmenu, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.editkeys:
			this.editBtns = !this.editBtns;
			TextView btn = (TextView) findViewById(R.id.status);

			if (btn != null) {
				if (this.editBtns)
					btn.setText(R.string.editingkeys);
				else
					btn.setText(R.string.controllermode);
			}
			this.renameButtons(this.editBtns);
			return true;
		case R.id.editcontrollers:
			Intent remotedlscreen = new Intent(this, RemoteDownloader.class);
			startActivity(remotedlscreen);
		default:
			return false;
		}

	}

	public void renameButtons(boolean showall) {
		Controller controller = this.controllermanager.getActiveController();

		{
			final Button b = (Button) findViewById(R.id.delCntrlBtn);
			if (this.editBtns)
				b.setVisibility(View.VISIBLE);
			else
				b.setVisibility(View.GONE);
		}

		TableLayout layout = (TableLayout) findViewById(R.id.BtnTable);;
		if (layout != null) {
			for (int i = 0; i < layout.getChildCount(); i++) {
				TableRow tr = (TableRow) layout.getChildAt(i);
				for (int j = 0; j < tr.getChildCount(); j++) {
					Button b = (Button) tr.getChildAt(j);
					if (controller != null) {
						KeyMapping keymapping = controller.getKey(b.getId());
						if (keymapping != null) {
							b.setVisibility(View.VISIBLE);
							b.setText(keymapping.btn_text);
						} else if (showall) {
							b.setText("-");
							b.setVisibility(View.VISIBLE);
						} else {
							b.setVisibility(View.INVISIBLE);
						}
					} else {
						b.setVisibility(View.INVISIBLE);
					}
				}
			}
		}

	}

	public void listControllers() {
		Spinner spinner = (Spinner) findViewById(R.id.ControllerSpinner);

		spinner.setOnItemSelectedListener(null);

		this.controllersAdapter.clear();
		this.controllersAdapter.putAll(this.controllermanager.getControllers());

		spinner.setOnItemSelectedListener(this);
	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int position,
			long id) {
		this.onControllSpinnerSelect(adapter, view, position, id);
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		// this.onControllSpinnerSelect(adapter, null, 0, 0);
	}

	private void onControllSpinnerSelect(AdapterView<?> adapter, View view,
			int position, long id) {

		ControllersSpinnerAdapter cadapter = (ControllersSpinnerAdapter) adapter
				.getAdapter();
		Controller controller = (Controller) cadapter.getItem(position);
		if (controller == null) {
			final boolean editBtns_ = editBtns;
			final EditText input = new EditText(this);
			new AlertDialog.Builder(this)
					.setTitle("New controller")
					.setMessage("New controller name")
					.setView(input)
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String controller_name = input.getText()
											.toString();
									Controller new_controller = new Controller(
											controller_name);
									controllermanager.put(new_controller);
									controllermanager
											.setActiveController(new_controller);

									Spinner spinner = (Spinner) findViewById(R.id.ControllerSpinner);
									listControllers();
									spinner.setSelection(controllersAdapter
											.getCount() - 2);
									renameButtons(editBtns_);
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).show();
		} else {
			this.controllermanager.setActiveController(controller);
			renameButtons(this.editBtns);
		}
	}
}
