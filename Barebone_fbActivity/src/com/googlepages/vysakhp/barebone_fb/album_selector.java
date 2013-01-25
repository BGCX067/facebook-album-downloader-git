package com.googlepages.vysakhp.barebone_fb;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.facebook.android.*;
import com.facebook.android.Facebook.*;
import java.io.*;
import java.util.*;
import org.json.*;

//album object contains graph id of the album and its name 	
class album implements java.lang.Comparable<album>
{
	String id;
	String name;
	
	album(String fb_name,String fb_id)
	{
		name = fb_name;
		id = fb_id;
	}

	public String toString()
	{
		return name;
	}

	public String getid()
	{
		return id;
	}
	
	//this method is used to alphabetically arrange albums
	public int compareTo(album another_album)
	{
		int result = this.name.compareToIgnoreCase(another_album.name);
		return result;
	}
}


public class album_selector extends ListActivity
{
	//initialise fb
	Facebook facebook = new Facebook("335838619816064");
	AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);
	
	//tag used when writing logs
	String TAG = this.toString();
	
	//for reading facebook access token and other prefs of this activity
	private SharedPreferences mPrefs;
	
	//fb permissions
	String[] permissions = {"friends_photos","user_photos"};

	ArrayList<album> album_list = null;
	
	//this list contains items matching the string typed in the search box
	ArrayList<album> filtered_list = new ArrayList<album>();
	JSONObject json, album_json;
	JSONArray child;
	String friend_id;
	String friend_name;
	Object selected_listitem;
	album selected_album;
	ListView lv;
	
	//search box
	EditText filterText;
	ProgressDialog dialog;
	
	//Intent which is send to last activity. it contains album_id,album_name and friend_name
	Intent final_intent;

	public void final_destination(String graph_id)
	{
		final_intent = new Intent();
		final_intent.putExtra("id",graph_id);
		final_intent.putExtra("name",selected_album.toString());
		final_intent.putExtra("friend_name",friend_name);
		final_intent.setComponent(new ComponentName(album_selector.this, download_photos.class));
		
		//without the FLAG_ACTIVITY_NEW_TASK the application will crash
		final_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Context context = getApplicationContext();
		context.startActivity(final_intent);
	}
	
	//pressing back button dismisses the progress dialog and finishes this activity
	public void onBackPressed()
    {
    	album_selector.this.dialog.dismiss();
    	album_selector.this.finish();
    }
	
	//loads menu items from xml
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu,menu);
		return true;
	}

	//code which handles the action for each menu item
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.settings:
			{
				Intent preferences_intent = new Intent();
				preferences_intent.setComponent(new ComponentName(album_selector.this,preferences.class));
				preferences_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplicationContext().startActivity(preferences_intent);
			
				return true;
			}
			
			case R.id.logout:
			{
				
				try
				{
					album_selector.this.facebook.logout(getApplicationContext());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}

				mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);

				SharedPreferences.Editor editor = mPrefs.edit();
       			editor.putString("access_token",null);
       			editor.putLong("access_expires",0);
				editor.putBoolean("logout",true);
       			editor.commit();
					
				finish();
				return true;
			}
			
			case R.id.about:
			{
				//shows our customized about dialog
				AboutDialog about = new AboutDialog(this);
				about.show();
			
				return true;
			}
			//lets deal with default case
			default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//locks the screen in portrait mode
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		filterText = (EditText) findViewById(R.id.search_box);
		filterText.setHint("Enter album name");
		
		dialog = ProgressDialog.show(album_selector.this, "", getText(R.string.loading));
		
		//pressing back button dismisses the progress dialog
		dialog.setCancelable(true);
		
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
		
		//get friend_name and friend_id from the intent which started this activity
		Intent starting_intent = getIntent();
		friend_id = starting_intent.getStringExtra("id");
		friend_name = starting_intent.getStringExtra("name");
				
		lv = getListView();
		
		//implements actions to done after receiving json object
		class meRequestListener extends BaseRequestListener
		{
			@Override
			public void onComplete(String response, Object state)
			{
				try
				{
					json = Util.parseJson(response);
					
					//albums are in the form of a json array
					child = json.getJSONArray("data");
					int total = child.length();
					int i;
					album_list = new ArrayList<album>(total);
					
					//loads album names and respective graph ids from json and save to the album_list
					for(i=0;i<=total -1;i++) 
					{
						album_json = child.getJSONObject(i);
						album_list.add(i,new album(album_json.getString("name"),album_json.getString("id")));
					}
					
					//sort the list alphabetically
					Collections.sort(album_list);
						
					album_selector.this.runOnUiThread(new Runnable()
					{             
						public void run()
						{ 	
							//dismiss progress dialog when the listview is filled
							album_selector.this.dialog.dismiss();
							
							setListAdapter(new ArrayAdapter<album>(getApplicationContext(), R.layout.list_item, album_list));
							
							//textwatcher listens to changes in search box and filters results
							album_selector.this.filterText.addTextChangedListener(new TextWatcher()
							{
								public void afterTextChanged(Editable s)
								{
									// Abstract Method of TextWatcher Interface.
								}

								public void beforeTextChanged(CharSequence s, int start, int count, int after)
								{
									// Abstract Method of TextWatcher Interface.
								}

								public void onTextChanged(CharSequence s, int start, int before, int count)
								{
									int textlength = album_selector.this.filterText.getText().length();
									album_selector.this.filtered_list.clear();

									/* Text entered in search box is checked first for its length (ie number of characters).
									 * Later the text in search box is checked with the list of all albums.
									 * Finally a new list is made which contains names of albums matching the pattern.									 * 
									 */
									for(int i=0;i<album_list.size();i++)
									{
										if(textlength<=album_list.get(i).name.length())
										{
											if(filterText.getText().toString().equalsIgnoreCase((String)album_list.get(i).name.subSequence(0,textlength)))
											{
												filtered_list.add((album)album_list.get(i));
											}
										}
									}
									
									//This is tricky. Here we bind the listview to the new and sorted list.
									album_selector.this.lv.setAdapter(new ArrayAdapter<album>(album_selector.this,R.layout.list_item,filtered_list));
								}
							});
							
							
							lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
							{
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
								{
									//listview contains textview widgets as children. this code obtains the the index of the textview in the listview
									selected_listitem = parent.getItemAtPosition((int)id);								
									
									//selected_listitem is of type Object. we have to convert it to album
									if(selected_listitem instanceof album)
									selected_album = (album)selected_listitem;								
																		
									//send the graph id of the album to last activity
									album_selector.this.final_destination(selected_album.getid());									
								}
							});   	
						}
					});
				}
					
				catch (JSONException e)
				{
					Log.e(TAG, "JSONEXception : " + e.getMessage());
				}
				
				catch (FacebookError e)
				{
					Log.e(TAG, "Facebook Error : " + e.getMessage());
				}

			}

		}
		
		//makes asynchronous request to facebook graph api
		//the actions to be performed after receiving json response is implemented above
		mAsyncRunner.request(friend_id + "/albums",new meRequestListener()); 
		
		if(!facebook.isSessionValid())
		{
	
			facebook.authorize(this, permissions, new DialogListener()
			{
				//save fb access token and its expiry values to prefernces of this activity	
				@Override
				public void onComplete(Bundle values)
				{
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString("access_token", facebook.getAccessToken());
					editor.putLong("access_expires", facebook.getAccessExpires());
					editor.commit();
				}

				@Override
				public void onFacebookError(FacebookError error)
				{
					Log.e(TAG, "Facebook Error : " + error.getMessage());
				}

				@Override
				public void onError(DialogError e)
				{
					Log.e(TAG,e.getMessage());
				}

				@Override
				public void onCancel()
				{

				}
			}); 
		}
	}
			
	@Override
	protected void onStart()
	{
		super.onStart();

		mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);
		String access_token = mPrefs.getString("access_token", null);
		long expires = mPrefs.getLong("access_expires", 0);

		if(access_token==null && expires==0)
		{
			this.finish();
		}

	}


	//required according facebook android sdk documentation	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
        super.onActivityResult(requestCode, resultCode, data);
        facebook.authorizeCallback(requestCode, resultCode, data);
    }	


}
