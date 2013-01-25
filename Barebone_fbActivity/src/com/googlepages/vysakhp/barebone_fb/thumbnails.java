package com.googlepages.vysakhp.barebone_fb;

import android.app.*;
import android.content.*;
import android.os.*;
import com.facebook.android.*;
import org.json.*;
import java.util.*;

class photo
{
	int position;
	String thumbnail;
	String low_res;
	String high_res;
	boolean download;
	
	photo(int position,String thumbnail,String low_res,String high_res,boolean download)
	{
		this.position = position;
		this.thumbnail = thumbnail;
		this.low_res = low_res;
		this.high_res = high_res;
		this.download = download;
	}
}

public class thumbnails extends Activity
{
	String album_id,album_name,friend_name;
	ArrayList<photo> photo_list = null;
	
	//initialise fb
	Facebook facebook = new Facebook("335838619816064");
	AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);
	JSONObject json,photo_json;
	JSONArray child;
	int total;
	
	SharedPreferences mPrefs;
	
	public void onCreate(Bundle savedinstance)
	{
		Intent starting_intent = getIntent();
		album_id = starting_intent.getStringExtra("id");
		album_name = starting_intent.getStringExtra("name");
		friend_name = starting_intent.getStringExtra("friend_name");
		
		//load preferences for this activity
		mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);

		//load fb access token and its expiry values
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);
		if(access_token!=null)
		{
			facebook.setAccessToken(access_token);
		}
		if(expires != 0)
		{
			facebook.setAccessExpires(expires);
		}
		
		//implements actions to done after receiving json object
		class meRequestListener extends BaseRequestListener
		{
			@Override
			public void onComplete(String response, Object state)
			{
				try
				{
					json = Util.parseJson(response);
					child = json.getJSONArray("data");
					
					total = child.length();
					photo_list = new ArrayList<photo>(total);
					
					for(int i=0;i<total;i++)
					{
						photo_json = child.getJSONObject(i);
						
						
					}
					
				}
				catch (JSONException e)
				{
					
				}
				catch (FacebookError e)
				{
						
				}

			}
		}
		
		mAsyncRunner.request(album_id + "/photos",new meRequestListener());
		
	}
}
