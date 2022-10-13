package Experiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import MainFBFI.IO;
import net.sf.json.JSONObject;

public class Http {
	public boolean success;
	public JSONObject response;

	public Http(String type, String URL, String parameter, String contentType, String authToken) {
		HttpURLConnection connection = null;
		OutputStreamWriter writer = null;
		BufferedReader reader = null;
		String response_string = "";
		try {
			URL url = new URL(URL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(type);
			connection.setConnectTimeout(120 * 1000);
			connection.setReadTimeout(60 * 1000);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestProperty("Content-Type", contentType);
			if (!authToken.isEmpty())
				connection.addRequestProperty("Authorization", "Bearer " + authToken);
			if (type.equals("POST")) {
				connection.setDoOutput(true);
				connection.setDoInput(true);
				writer = new OutputStreamWriter(connection.getOutputStream());
				writer.write(parameter);
				writer.flush();
				writer.close();
			} else
				connection.connect();
			int ResponseCode = connection.getResponseCode();
			if (ResponseCode == 200) {
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
				String line;
				while ((line = reader.readLine()) != null) {
					response_string += line;
				}
				response = JSONObject.fromObject(response_string);
				success = true;
				return;
			} else {
				IO.Output("Got an error code: " + ResponseCode + " when " + type + " " + URL);
				if(type.equals("POST"))
					IO.Output("Parameter is: " + parameter);
				success = false;
				return;
			}
		} catch (Exception e) {
			IO.Output("Got an exception" + " when " + type + " " + URL);
			if(type.equals("POST")) 
				IO.Output("Parameter is: " + parameter);
			success = false;
			return;
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			connection.disconnect();
		}
	}
}
