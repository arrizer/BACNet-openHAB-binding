package org.openhab.binding.bacnet.driver;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONable {
	public JSONObject toJSON() throws JSONException;
}
