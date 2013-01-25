package com.googlepages.vysakhp.barebone_fb;


import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.facebook.android.*;
import com.facebook.android.Facebook.*;
import java.io.*;
import java.util.*;
import org.json.*;

//person object contains graph id of the album and its name
class person implements java.lang.Comparable<person>
{
	String name;
	String id;

	person(String fb_name,String fb_id)
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
	
	//this method is used to alphabetically arrange persons
	public int compareTo(person another_person)
	{
		int result = this.name.compareToIgnoreCase(another_person.name);
		return result;
	}
}

public class Barebone_fbActivity extends ListActivity
{
	//initialise fb
    Facebook facebook = new Facebook("335838619816064");
    AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);
    
    //tag used when writing logs
    String TAG = Barebone_fbActivity.this.toString();
	
    //for reading facebook access token and other prefs of this activity
	private SharedPreferences mPrefs;
    
	//fb permissions
    String[] permissions = {"friends_photos","user_photos"};
	String access_token=null;
	long expires;
	boolean show_login_dialog;
	boolean logout;
	ArrayList<person> friends_list = null;
	
	//this list contains items matching the string typed in the search box
	ArrayList<person> filtered_list = new ArrayList<person>();
	Object selected_listitem;
	person selected_friend;
	JSONObject json;
	JSONObject friend;
	JSONArray child;
	ListView lv;
	
	//search box
	EditText filterText;
	ProgressDialog dialog;
		
	//Intent which is send to second activity. it contains friend_id and friend_name
	Intent album_intent;
	
	public void start_album_selector(String graph_id)
	{
		album_intent = new Intent();
		album_intent.putExtra("id",graph_id);
		album_intent.putExtra("name",selected_friend.toString());
		album_intent.setComponent(new ComponentName(Barebone_fbActivity.this, album_selector.class));
		
		//without the FLAG_ACTIVITY_NEW_TASK the application will crash
		album_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Context context = getApplicationContext();
		context.startActivity(album_intent);
	}
	
	//the progress dialog is dismissed if by pressing back button
	public void onBackPressed()
    {
    	Barebone_fbActivity.this.dialog.dismiss();
    	Barebone_fbActivity.this.finish();
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
				preferences_intent.setComponent(new ComponentName(Barebone_fbActivity.this,preferences.class));
				preferences_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplicationContext().startActivity(preferences_intent);
			
				return true;
			}
			
			case R.id.logout:
			{
				
				try
				{
					Barebone_fbActivity.this.facebook.logout(getApplicationContext());
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
					
				
				new AlertDialog.Builder(this).setMessage("You have logged out successfully. Please relaunch this application to login again.") 
				.setTitle("Logout") 
				.setCancelable(false) 
				.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{ 
					public void onClick(DialogInterface dialog, int whichButton)
					{
						finish();
					} 
				}).show();
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
	
	//required according facebook android sdk documentation
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		facebook.authorizeCallback(requestCode, resultCode, data);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	//locks the screen in portrait mode
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				
    	super.onCreate(savedInstanceState);
	
    	setContentView(R.layout.main);
		filterText = (EditText) findViewById(R.id.search_box);
		filterText.setHint("Enter friend's name");
		
		dialog = ProgressDialog.show(Barebone_fbActivity.this, "", getText(R.string.loading));

    	//pressing back button dismisses the progress dialog
    	dialog.setCancelable(true);
			
		//checks the internet connection
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (!(networkInfo != null && networkInfo.isConnected()))
		{
			dialog.dismiss();
			new AlertDialog.Builder(this).setMessage(getText(R.string.internet_connection_dialog_message)) 
			.setTitle(getText(R.string.internet_connection_dialog_title)) 
			.setCancelable(false) 
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{ 
				public void onClick(DialogInterface dialog, int whichButton)
				{
					finish();
				} 
			}).show();
			
		}
		
	
    	//load preferences for this activity
    	mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);
    	access_token = mPrefs.getString("access_token", null);
    	expires = mPrefs.getLong("access_expires", 0);
		
		getSharedPreferences("COMMON",MODE_PRIVATE).edit()
		.putBoolean("logout",false)
		.commit();
    	
		SharedPreferences login_prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		show_login_dialog=login_prefs.getBoolean("show_login_dialog",false);
    	if(access_token!=null)
		{
			facebook.setAccessToken(access_token);
			//Log.e(TAG,access_token);
		}
		
    	if(expires != 0)
		{
			facebook.setAccessExpires(expires);
			//Log.e(TAG,String.valueOf(expires));
		}
		
	
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
					
    				//friends are in the form of a json array
    				child = json.getJSONArray("data");
    				int total = child.length();
    				int i;
    			
    				friends_list = new ArrayList<person>(total);
    				
    				//loads album names and respective graph ids from json and save to the friends_list
    				for(i=0;i<=child.length() -1;i++) 
    				{
    					friend = child.getJSONObject(i);
    					friends_list.add(i,new person(friend.getString("name"),friend.getString("id")));
    				}
    			
    				//sort the list alphabetically
    				Collections.sort(friends_list);

    				Barebone_fbActivity.this.runOnUiThread(new Runnable()
    				{             
    					public void run()
    					{ 	
    						//dismiss progress dialog when the listview is filled
    						Barebone_fbActivity.this.dialog.dismiss();
    						setListAdapter(new ArrayAdapter<person>(getApplicationContext(), R.layout.list_item, friends_list));
    						
    						//textwatcher listens to changes in search box and filters results
							Barebone_fbActivity.this.filterText.addTextChangedListener(new TextWatcher()
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
									int textlength = Barebone_fbActivity.this.filterText.getText().length();
									
									//Removes all elements from this ArrayList, leaving it empty.
									Barebone_fbActivity.this.filtered_list.clear();
									
									/* Text entered in search box is checked first for its length (ie number of characters).
									 * Later the text in search box is checked with the list of all friends.
									 * Finally a new list is made which contains names of friends matching the pattern.									 * 
									 */
									for(int i=0;i<friends_list.size();i++)
									{
										if(textlength<=friends_list.get(i).name.length())
										{
											if(filterText.getText().toString().equalsIgnoreCase((String)friends_list.get(i).name.subSequence(0,textlength)))
											{
												filtered_list.add((person)friends_list.get(i));
											}
										}
									}
									
									//This is tricky. Here we bind the listview to the new and sorted list.
									Barebone_fbActivity.this.lv.setAdapter(new ArrayAdapter<person>(Barebone_fbActivity.this,R.layout.list_item,filtered_list));
								}
							});
							
    						lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
    						{
    							public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
    							{
    								//listview contains textview widgets as children. this code obtains the the index of the textview in the listview
    								selected_listitem = parent.getItemAtPosition((int)id);
    								
    								//selected_listitem is of type Object. we have to convert it to album
    								if(selected_listitem instanceof person)
    								selected_friend = (person)selected_listitem;								
    								//Toast.makeText(getApplicationContext(),selected_friend.getid(),Toast.LENGTH_LONG).show();
    								
    								//send the graph id of the friend to last activity
    								Barebone_fbActivity.this.start_album_selector(selected_friend.getid());
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
    				Log.e(TAG, "Facebook Error : " + e.getMessage(),e.fillInStackTrace());
		
    			}
    		}
        }
		
		class myDialogListener implements DialogListener
		{
			//save fb access token and its expiry values to prefernces of this activity
       		@Override
       		public void onComplete(Bundle values)
			{
				mPrefs=getSharedPreferences("COMMON",MODE_PRIVATE);
				SharedPreferences.Editor editor = mPrefs.edit();
				access_token=facebook.getAccessToken();
				expires=facebook.getAccessExpires();
				//access_token=values.getString("TOKEN");
				//expires=Long.getLong(values.getString("EXPIRES"));

				editor.putString("access_token", access_token);
				editor.putLong("access_expires", expires);
				editor.commit();

				//makes asynchronous request to facebook graph api
				mAsyncRunner.request("me/friends",new meRequestListener());

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
				new AlertDialog.Builder(Barebone_fbActivity.this).setMessage(getText(R.string.invalid_login_dialog_message)) 
				.setTitle(getText(R.string.invalid_login_dialog_title)) 
				.setCancelable(false) 
				.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
				{ 
					public void onClick(DialogInterface dialog, int whichButton)
					{
						finish();
					} 
				}).show();
			}
		}
        
		
        if(!facebook.isSessionValid())
		{
       		dialog.dismiss();
			
			if(show_login_dialog)
			{
				facebook.authorize(this, permissions,facebook.FORCE_DIALOG_AUTH, new myDialogListener());
			}
			else
			{
				facebook.authorize(this, permissions, new myDialogListener());
			}
        
		}
		//makes asynchronous request to facebook graph api
		mAsyncRunner.request("me/friends",new meRequestListener());
		
	}
    
	@Override
	protected void onStart()
	{
		super.onStart();
		
		mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);
		access_token = mPrefs.getString("access_token", null);
		expires = mPrefs.getLong("access_expires", 0);
		logout=mPrefs.getBoolean("logout",false);
		if(logout==true && access_token==null)
		{
					
			new AlertDialog.Builder(this).setMessage("You have logged out successfully. Please relaunch this application to login again.") 
			.setTitle("Logout") 
			.setCancelable(false) 
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{ 
				public void onClick(DialogInterface dialog, int whichButton)
				{
					finish();
				} 
			}).show();
			
		}
	}
    protected void onResume()
	{
		super.onResume();
		mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);
		access_token = mPrefs.getString("access_token", null);
		expires = mPrefs.getLong("access_expires", 0);
		logout=mPrefs.getBoolean("logout",false);
		if(logout==true && access_token==null)
		{
			
			new AlertDialog.Builder(this).setMessage("You have logged out successfully. Please relaunch this application to login again.") 
			.setTitle("Logout") 
			.setCancelable(false) 
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{ 
				public void onClick(DialogInterface dialog, int whichButton)
				{
					finish();
				} 
			}).show();

		}
	}
}
