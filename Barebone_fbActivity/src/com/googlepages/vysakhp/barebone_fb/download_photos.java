package com.googlepages.vysakhp.barebone_fb;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.media.*;
import com.facebook.android.*;
import com.facebook.android.Facebook.*;
import com.googlepages.vysakhp.barebone_fb.utils.DownloadThread;
import com.googlepages.vysakhp.barebone_fb.utils.DownloadTask;
import com.googlepages.vysakhp.barebone_fb.utils.DownloadThreadListener;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
import android.preference.*;
import android.opengl.*;


public class download_photos extends Activity implements DownloadThreadListener
{

	String album_id, album_name,friend_name,root_folder,file_name,sub_path;
	
	//real album and friend name may contain characters not suitable for file names.
	String legal_friend_name,legal_album_name; 
	
	String first_image;
	
	String max_photos_in_album = "1000";
	//points to folders in sdcard
	File path,fully_qualified_file_name,resume_file;

	JSONObject json = null;
	JSONArray child = null;
	JSONObject photo_json = null;
	
	//tag used when writing logs
	String TAG = this.toString();
	TextView mtext,progress_text;
	ProgressBar progress_download;
	Button resume_pause;
	
	int total_files_to_download;
	int completed_downloads;
	int initial_value,final_value;
	
	DownloadImageTask mytask;
	DownloadThread myThread;
	
	private DownloadThread downloadThread;
	private Handler handler;
	
	private SharedPreferences mPrefs;//for reading facebook access token and other prefs of this activity
	
	boolean dl_high_res_pics,paused,completed;
	String[] permissions = {"friends_photos","user_photos"}; //fb permissions
	public ArrayList<String> links = null;//list of links to photos
	public List<String> path_segments = null;//elements of the list are sections of image url between forward slashes
	
	//initialise fb
	Facebook facebook = new Facebook("335838619816064");
	AsyncFacebookRunner mAsyncRunner = new AsyncFacebookRunner(facebook);

	public synchronized void writeProgress(int current_file,int total_files)
	{
		try
		{
			
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(resume_file, false)));
			out.writeInt(current_file);
			out.writeInt(total_files);
			out.flush();
			out.close();
	
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			Log.e(TAG,"writeProgress resume.txt not found.");
		}
		catch (IOException ex)
		{
			Log.e(TAG,"Unable to create resume.txt.");
		}
		
	}
	
	public int[] readProgress()
	{
		int progress[] = new int[2];
		try
		{
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(resume_file)));
			progress[0] = in.readInt();
			progress[1] = in.readInt();
			in.close();
		}
		catch (FileNotFoundException e)
		{
			Log.e(TAG,"readProgress file not found.");
		}
		catch (IOException e)
		{
			Log.e(TAG,e.getMessage());
		}
		return progress;
	
	}
	
	
	//asynctask calls downloadmanager to download photos
	public class DownloadImageTask extends AsyncTask<ArrayList,Integer,Boolean>
	{
		
		protected void onCancelled()
		{
			writeProgress(completed_downloads,total_files_to_download);
			
		}
		protected void onPostExecute(Boolean result)
		{
			completed = true;
			writeProgress(completed_downloads,total_files_to_download);
			if(resume_pause.VISIBLE!=View.GONE)
				resume_pause.setVisibility(View.GONE);
		}
		protected void onPreExecute()
		{
			super.onPreExecute();
		//	mProgressDialog.show();
		
		}
		
		protected void onProgressUpdate(Integer...progress)
		{
			super.onProgressUpdate(progress);
			int total_files = progress[1];
			int current_file = progress[0];
			progress_download.incrementProgressBy(1);
			progress_download.setMax(total_files);
			
			if(current_file<total_files)
			{
				progress_text.setText("Downloading " + Integer.toString(current_file) + " of " + Integer.toString(total_files) + ".");
			}
			else
			{
				progress_text.setText("Completed.");
				resume_pause.setVisibility(View.GONE);
				//writeProgress(
				//viewimages.setEnabled(true);
			}
		//	mProgressDialog.setProgress(progress[0]);
			progress_download.setProgress(current_file);
		
		}
		
		protected Boolean doInBackground(ArrayList... params)
		{
			download_photos.this.runOnUiThread(new Runnable()
			{
				public void run()
				{
					mtext.setText(getText(R.string.download_textview_message_1) + " " + path.toString() + ". ");
								
				}
			});
			
			if(resume_file.exists())
			{
				initial_value = readProgress()[0];
			}
			else
			{
				initial_value = 1;
			}
			
			for(int i=initial_value-1;i<links.size();i++)
			{
				//asynctask expects more than one ArrayList<String> item, but we are sending only one, which is params[0]
				Uri imageuri = Uri.parse( params[0].get(i).toString());
				URL imageurl=null;
				HttpURLConnection connection=null;
				total_files_to_download = links.size();
				completed_downloads = i + 1;
				try
				{
					imageurl = new URL(params[0].get(i).toString());
					connection = (HttpURLConnection)imageurl.openConnection();
					connection.connect();
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//extracts the real file name of the photo from url
				path_segments = imageuri.getPathSegments();
				int total_segments = path_segments.size();
				file_name = path_segments.get(total_segments -1);
				
				path.mkdirs();
				
				//if(i==0)
				//	first_image = path.toString() + "/" + file_name;
				
				InputStream input;
				OutputStream output;
				try
				{
					input = new BufferedInputStream(imageurl.openStream());
					fully_qualified_file_name = new File(path,file_name);
					output = new BufferedOutputStream(new FileOutputStream(fully_qualified_file_name));
					byte data[] = new byte[1024];
					int count;
		            while ((count = input.read(data)) != -1)
		            {
		                output.write(data, 0, count);
		            }
		            output.flush();
		            output.close();
		            input.close();
					connection.disconnect();
					
					new folder_scanner(getApplicationContext(),fully_qualified_file_name);
					
					publishProgress(completed_downloads,total_files_to_download);
					if(this.isCancelled())
					{
						writeProgress(completed_downloads,total_files_to_download);
						break;
					}
					
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//creates required folders and subfolders if they do not exist already
				//boolean success = path.mkdirs();
				
				//makes request to download photos
				//DownloadManager.Request request = new DownloadManager.Request(imageuri);
				
				//set path for downloads
				//request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,sub_path);
				
				//request.setDescription("Downloaded using Facebook Album Downloader");
					
				//DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
			
				//download is enqueue in download list. it returns unique id for each download
				//download_id = dm.enqueue(request);
					
			}
			//returns the unique id. we are not using this id
			return true;
		}
	}
	
	//pressing back button finishes this activity
	public void onBackPressed()
	{
		//writeProgress(completed_downloads,total_files_to_download);
		downloadThread.requestStop();
		super.onBackPressed();
		
		
		//download_photos.this.finish();
	}
	
	//load the menu from xml
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
				preferences_intent.setComponent(new ComponentName(download_photos.this,preferences.class));
				preferences_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getApplicationContext().startActivity(preferences_intent);
			
				return true;
			}
			case R.id.logout:
			{
				
				try
				{
					download_photos.this.facebook.logout(getApplicationContext());
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
	
	protected void onPause()
	{
		super.onPause();
		downloadThread.requestStop();
	}
	protected void onStop()
	{
		super.onStop();
		downloadThread.requestStop();
	}
	
	protected void onDestroy()
	{
		super.onDestroy();
		downloadThread.requestStop();
	}
	
	@Override
	public void handleDownloadThreadUpdate()
	{
		handler.post(new Runnable()
		{
			
			@Override
			public void run()
			{
				//total_files_to_download = downloadThread.getTotalQueued();
				total_files_to_download = links.size();
				if(downloadThread!=null)
				{
					completed_downloads = downloadThread.getTotalCompleted() + initial_value;
				}
				else
				{
					completed_downloads = readProgress()[0];
				}
				
				progress_download.setMax(links.size());
				//progress_download.incrementProgressBy(1);
				progress_download.setProgress(0); // need to do it due to a ProgressBar bug
				progress_download.setProgress(completed_downloads);
				
				progress_text.setText("Downloading " + Integer.toString(completed_downloads + 1) + " of " + Integer.toString(links.size()) + ".");
				
				//writeProgress(completed_downloads,total_files_to_download);
				
				if(completed_downloads == total_files_to_download)
				{
					completed = true;
					writeProgress(completed_downloads,total_files_to_download);
					if(resume_pause.VISIBLE!=View.GONE)
						resume_pause.setVisibility(View.GONE);
					downloadThread.requestStop();
					progress_text.setText("Completed.");
				}
			}
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//locks the screen in portrait mode
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.downloader);
		mtext = (TextView)findViewById(R.id.mtext);
		progress_text = (TextView)findViewById(R.id.progress_text);
		progress_download = (ProgressBar)findViewById(R.id.progress_download);
		//resume capability
		
		paused = false;
		resume_pause = (Button)findViewById(R.id.resume);
		
		// Create and launch the download thread
        downloadThread = new DownloadThread(this);
        downloadThread.start();
        
        // Create the Handler. It will implicitly bind to the Looper
        // that is internally created for this thread (since it is the UI thread)
        handler = new Handler();
		
		resume_pause.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(!paused)
				{
					paused=true;
					resume_pause.setText("Resume");
					//writeProgress(completed_downloads,total_files_to_download);
					downloadThread.requestStop();
					downloadThread = null;
	
				}
				else
				{
					resume_pause.setText("Pause");
					paused=false;
					
					initial_value = readProgress()[0];
					final_value = links.size();
					
					for(int i=initial_value;i<links.size();i++)
					{
						downloadThread = new DownloadThread(download_photos.this);
				        downloadThread.start();
						downloadThread.enqueueDownload(new DownloadTask(links.get(i),path,i+1,links.size(),download_photos.this));

					}
				}
	
			}
		});
		
		//load preferences for this activity
		mPrefs = getSharedPreferences("COMMON",MODE_PRIVATE);
		
		//This declaration is solely meant for reading the checkbox preference for downloading high res pics.
		SharedPreferences dl_prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		//The global variable is set after reading the  checkbox preference.
		dl_high_res_pics = dl_prefs.getBoolean("download_high_res",false);
		
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
		
		//get friend_name and album_name from the intent which started this activity
		Intent starting_intent = getIntent();
		album_id = starting_intent.getStringExtra("id");
		album_name = starting_intent.getStringExtra("name");
		friend_name = starting_intent.getStringExtra("friend_name");
		
		//real album and friend name may contain characters not suitable for file names.
		//regex pattern includes most of the invalid characters in file names
		legal_album_name = album_name.replaceAll("[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
		legal_friend_name = friend_name.replaceAll("[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
		
		//initialise the directory structure for download
		path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + getText(R.string.app_name) + "/" + legal_friend_name + "/" + legal_album_name);
		if(dl_high_res_pics)
			path = new File(path.toString() + "/" + getText(R.string.folder_name_high_res));
			
		resume_file = new File(path,"resume.txt");
		//implements actions to done after receiving json object
		class meRequestListener extends BaseRequestListener
		{
			@Override
			public void onComplete(String response, Object state)
			{
				try
				{
					Log.i(TAG,response);
					json = Util.parseJson(response);
					
					//photos are in the form of a json array
					child = json.getJSONArray("data");
					
					int total = child.length();
					
					//contains links to photos
					links = new ArrayList<String>(total);
					
					//adds link to each photo to our list after replacing the "https" from url
					//DownloadManager does not support https in gingerbread
					for(int i=0;i<total;i++)
					{
						photo_json= child.getJSONObject(i);
						
						if(dl_high_res_pics)
						{
							JSONArray image_set = photo_json.getJSONArray("images");
							
							//highest resolution picture has the index zero in the images jsonarray
							JSONObject highest_res_pic = image_set.getJSONObject(0);
							String http_replaced = highest_res_pic.getString("source").replaceFirst("https","http");
							links.add(i,http_replaced);
						}
						else
						{
							//source property of the json object points to the photo's link
							String http_replaced = photo_json.getString("source").replaceFirst("https","http");
							links.add(i,http_replaced);	
						}
					
					}
					
					download_photos.this.runOnUiThread(new Runnable()
					{
						public void run()
						{
						//	mytask = new DownloadImageTask();
						//	mytask.execute(links);
						//start downloading using asynctask
						//new DownloadImageTask().execute(links);
							//downloadThread.setPath(path);
							//downloadThread.setWholeTasks(links.size());
							if(resume_file.exists())
							{
								
								initial_value = readProgress()[0];
								final_value = links.size();
								//case if the task is already completed
								if(initial_value == final_value)
								{
									completed = true;
									resume_pause.setVisibility(View.GONE);
									
									progress_download.setMax(final_value);
									progress_download.setProgress(0);	//bug in progress bar
									progress_download.setProgress(initial_value);
									
									progress_text.setText("Completed.");
									mtext.setText(getText(R.string.download_textview_message_1) + " " + path.toString() + ". ");
									
								}
								
								//case if some of the photos are already downloaded
								else
								{
									progress_download.setMax(links.size());
									//progress_download.setProgress(0);	//bug in progress bar
									progress_download.setProgress(initial_value);
									
									//N.B if i= initial_value -1, then one image will be downloaded again. so to save cost we start with i = initial_value
									for(int i=initial_value;i<links.size();i++)
									{
										mtext.setText(getText(R.string.download_textview_message_1) + " " + path.toString() + ". ");
										//downloadThread.setRunningTask(i + 1);
										downloadThread.enqueueDownload(new DownloadTask(links.get(i),path,i+1,final_value,download_photos.this));
									}
								}
							}
							
							//case if the task is entirely new task
							else
							{
								for(int i=0;i<links.size();i++)
								{
									mtext.setText(getText(R.string.download_textview_message_1) + " " + path.toString() + ". ");
									//downloadThread.setRunningTask(i + 1);
									downloadThread.enqueueDownload(new DownloadTask(links.get(i),path,i+1,links.size(),download_photos.this));
								}
							}
							
						
						}
					});
				}
				
				catch(JSONException ex)
				{
					Log.e(TAG,"JSONEXception : " + ex.getMessage());
				}
			}	
		}
		
		
		//makes asynchronous request to facebook graph api
		//the actions to be performed after receiving json response is implemented above
	
		Bundle parameters = new Bundle();
		
		//facebook returns only 25 photos when limit is not specified.
		parameters.putString("limit",max_photos_in_album);
		if(!completed)
			mAsyncRunner.request(album_id + "/photos",parameters,new meRequestListener());
		
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
					//do nothing LOL :-)
				}
			});	
		}
	}
	
	//required according facebook android sdk documentation
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		facebook.authorizeCallback(requestCode, resultCode, data);
	}	
}
	

