package pl.poznan.put.airc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class RemoteWebViewClient extends WebViewClient {
	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String urlConnection) {
		// here you will use the url to access the headers.
		// in this case, the Content-Length one
		if (urlConnection.startsWith(RemoteDownloader.lirc_confs_url)) {
			URL url;
			URLConnection conexion;
			String content_type = null;
			try {
				url = new URL(urlConnection);
				conexion = url.openConnection();
				conexion.setConnectTimeout(3000);
				conexion.connect();
				content_type = conexion.getContentType();
			} catch (Exception e) {
			}

			if (content_type.equals("text/plain")) {
				String content = "";
				HttpGet httpGet = new HttpGet(urlConnection);
				HttpClient httpClient = new DefaultHttpClient();
				// this receives the response
				HttpResponse response;
				try {
					response = httpClient.execute(httpGet);
					if (response.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = response.getEntity();
						if (entity != null) {
							InputStream inputStream = entity.getContent();
							content = convertToString(inputStream);
							Context context = view.getContext();

							ControllerManager
									.save_remote_conf(
											context,
											urlConnection
													.replaceFirst(
															RemoteDownloader.lirc_confs_url,
															""), content);

							Toast toast = Toast.makeText(context,
									"Downloaded remote from " + urlConnection,
									Toast.LENGTH_SHORT);
							toast.show();
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
			{
				view.loadUrl(urlConnection);
			}
		}

		return true;
	}

	public String convertToString(InputStream inputStream) {
		StringBuffer string = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream));
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				string.append(line + "\n");
			}
		} catch (IOException e) {
		}
		return string.toString();
	}
}
