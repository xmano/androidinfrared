package pl.poznan.put.airc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import java.util.UUID;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class ControllersSpinnerAdapter extends LinkedHashMap<UUID, Controller>
		implements SpinnerAdapter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Context context;
	private final ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

	public ControllersSpinnerAdapter(Context context) {
		super();
		this.context = context;
	}
	
	public void notifyObservers()
	{
		for(DataSetObserver observer : this.observers)
		{
			observer.onChanged();
		}
	}

	@Override
	public int getCount() {
		return super.size()+1;
	}

	@Override
	public Object getItem(int position) {
		if (position < super.size()) {
			return super.values().toArray()[position];
		}

		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position < super.size()) {
			return ((UUID) super.keySet().toArray()[position])
				.getMostSignificantBits();
		}
		
		return 0;
	}

	@Override
	public int getItemViewType(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return this.getCustomView(position, convertView, parent);
	}

	@Override
	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		observers.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		observers.remove(observer);
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return this.getCustomView(position, convertView, parent);
	}

	private View getCustomView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(this.context);
		View row = inflater.inflate(android.R.layout.simple_spinner_item,
				parent, false);
		TextView label = (TextView) row.findViewById(android.R.id.text1);
		Controller controller = (Controller) this.getItem(position);
		if (controller == null) {
			label.setText(this.context.getString(R.string.new_controller));
		} else {
			label.setText(controller.getName());
		}

		return row;
	}
	
	@Override
	public Controller put(UUID key, Controller value) {
		super.put(key, value);
		this.notifyObservers();
		return value;
	}
	
	@Override
	public void putAll(Map<? extends UUID, ? extends Controller> map) {
		super.putAll(map);
		this.notifyObservers();
	}
	
	@Override
	public void clear()
	{
		super.clear();
		this.notifyObservers();
	}

}
