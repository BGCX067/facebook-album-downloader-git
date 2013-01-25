package com.googlepages.vysakhp.barebone_fb;

import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.preference.*;
import android.view.*;
import android.widget.*;



public class preferences extends PreferenceActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//locks the screen in portrait mode
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		

	}
		
}


