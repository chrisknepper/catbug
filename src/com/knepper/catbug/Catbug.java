package com.knepper.catbug;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

public class Catbug {
	
	protected static Boolean debug = true;
	private static ThreadedRequest dl;
	public static String url;
		
	protected static String read(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		BufferedReader r = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
		StringBuilder total = new StringBuilder();
		String line;
		while ((line = r.readLine()) != null) {
		    total.append(line);
		}
		return total.toString();
	}
	
	protected static String getQuery(Map<String, String> params) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for(Map.Entry<String, String> pair : params.entrySet()) {
			if(first)
				first = false;
			else
				result.append("&");
			
			result.append(URLEncoder.encode(pair.getKey(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(/*(String)*/pair.getValue(), "UTF-8"));
		}
		
		return result.toString();
	}
	
	public static boolean connect(Map<String, String> params, ProgressDialog loading_object, Activity activity) {
		if(isNetworkAvailable(activity)) {
			if(loading_object != null) {
				loading_object.setTitle("Connecting");
				loading_object.setMessage("Communicating with server...");
				loading_object.show();				
			}
			dl = new ThreadedRequest();
			dl.setListener((CatbugCallback) activity);
			dl.execute(params);
			return true;
		}
		else {
			return false;
		}
	}
	
	public static boolean isNetworkAvailable(Context context){
	    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    if ((activeNetworkInfo != null)&&(activeNetworkInfo.isConnected())){
	        return true;
	    }else{
	        return false;
	    }
	}

}

class ThreadedRequest extends AsyncTask<Map<String, String>, Void, String> {
	
	CatbugCallback mListener;
	
	@Override
	protected String doInBackground(Map<String, String>... params) {
		try {
			return downloadUrl(params[0]);
		} catch(IOException e) {
			return "Unable to connect to the server";
		}
	}
	
	// onPostExecute displays the results of the AsyncTask.
	@Override
	protected void onPostExecute(String result) {
		mListener.CatbugSays(result);
		if(Catbug.debug) {
			Log.d("Catbug", "The server says: " + result);
		}
	}
	
	public void setListener(CatbugCallback listener) {
		mListener = listener;
	}
	
	private String downloadUrl(Map<String, String> params) throws IOException {
		InputStream is = null;
		int len = 20;
		
		try {
			URL url = new URL(Catbug.url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("POST");
			conn.setDoInput(true);
			conn.setDoOutput(true);
			
			OutputStream os = conn.getOutputStream();
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(os, "UTF-8"));
			writer.write(Catbug.getQuery(params));
			writer.close();
			os.close();
			//Start the query
			conn.connect();
			int response = conn.getResponseCode();
			if(Catbug.debug) {
				Log.d("Catbug", "The response is: " + response);
			}
			is = conn.getInputStream();
			
			//Convert the InputStream into a string
			String contentAsString = Catbug.read(is, len);
			return contentAsString;
		}
		finally {
			if(is != null) {
				is.close();
			}
		}
	}

}
