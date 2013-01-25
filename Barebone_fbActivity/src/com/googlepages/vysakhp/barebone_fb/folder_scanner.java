package com.googlepages.vysakhp.barebone_fb;

import java.io.File;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class folder_scanner implements MediaScannerConnectionClient
{

	private MediaScannerConnection mMs;
	private File mFile;

	public folder_scanner(Context context, File f)
	{
		mFile = f;
		mMs = new MediaScannerConnection(context, this);
		mMs.connect();
	}

	@Override
	public void onMediaScannerConnected()
	{
		//scans only one file at a time. requires for loop to scan a folder.
		mMs.scanFile(mFile.getAbsolutePath(), null);
	}

	@Override
	public void onScanCompleted(String path, Uri uri)
	{
		mMs.disconnect();
	}

}
