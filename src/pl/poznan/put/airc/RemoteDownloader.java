package pl.poznan.put.airc;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class RemoteDownloader extends Activity {
	public static final String lirc_confs_url = "http://lirc.sourceforge.net/remotes/";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloader);
		
		WebView webview = (WebView) this.findViewById(R.id.webView);
		if(webview != null) {
			
			WebSettings websettings = webview.getSettings();
			websettings.setJavaScriptEnabled(false);
			websettings.setLoadsImagesAutomatically(false);
			webview.setWebViewClient(new RemoteWebViewClient());
			webview.loadUrl(lirc_confs_url);
			
		} else
		{
			Log.wtf("AIRC", "webview failed?");
		}
	}

}
