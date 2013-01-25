package com.googlepages.vysakhp.barebone_fb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.widget.TextView;
import android.view.ViewGroup.*;

public class AboutDialog extends Dialog
{
	private static Context mContext = null;
	public AboutDialog(Context context)
	{
		super(context);
		mContext = context;
	}

	/**
	 * This is the standard Android on create method that gets called when the activity initialized.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.about);
		//	getWindow().setLayout(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
				
		TextView tv = (TextView)findViewById(R.id.info_text);
		//tv.setHeight(LayoutParams.WRAP_CONTENT);
		//tv.setText(Html.fromHtml(readRawTextFile(R.raw.title)));
		tv = (TextView)findViewById(R.id.info_text);
				
		tv.setText(Html.fromHtml(readRawTextFile(R.raw.info)));
		tv.setLinkTextColor(Color.WHITE);
		Linkify.addLinks(tv, Linkify.ALL);
	}

	public static String readRawTextFile(int id)
	{
		InputStream inputStream = mContext.getResources().openRawResource(id);
		InputStreamReader in = new InputStreamReader(inputStream);
		BufferedReader buf = new BufferedReader(in);
		String line;
		StringBuilder text = new StringBuilder();
		try
		{
			while (( line = buf.readLine()) != null) text.append(line);
		}
		catch (IOException e)
		{
			return null;
		}
		return text.toString();
	}

}

