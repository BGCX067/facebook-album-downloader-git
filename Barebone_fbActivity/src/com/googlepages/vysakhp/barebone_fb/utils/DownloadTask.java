package com.googlepages.vysakhp.barebone_fb.utils;

import java.net.*;
import android.net.Uri;
import android.util.Log;

import java.io.*;

import android.content.*;
import com.googlepages.vysakhp.barebone_fb.folder_scanner;

//courtsey http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
public class DownloadTask implements Runnable
{
	String myuri, file_name;
	File localPath, fully_qualified_file_name;
	//List<String> path_segments;
	Uri imageuri;
	URL imageurl;
	HttpURLConnection connection;
	InputStream input;
	OutputStream output;
	Context mContext;
	int current_task, total_tasks;
	File path, resume_file;
	String TAG = DownloadTask.class.getSimpleName();
	
//	public void setCurrentTask(int current_task)
//	{
//		this.current_task = current_task + 1;
//	}
//	
//	public void setTotalTasks(int total_tasks)
//	{
//		this.total_tasks = total_tasks;
//	}
//	
//	public void setPath(File path)
//	{
//		this.path = path;
//	
//	}
	
	public synchronized void writeProgress(int current_file,int total_files)
	{
		
		try
		{
			//DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(resume_file)));
			//int i = in.readInt();
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(localPath,"resume.txt"))));
			
			out.writeInt(current_file);
			out.writeInt(total_files);
			out.flush();
			out.close();
		
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			Log.e(TAG,"writeProgress resume.txt not found.");
			Log.e(TAG,e.getMessage());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			Log.e(TAG,"Unable to create resume.txt.");
		}

	}
	
	public DownloadTask(String uri, File localPath, int currentTask, int totalTasks, Context mContext)
	{
		this.myuri = uri;
		this.localPath = localPath;
		this.mContext = mContext;
		this.current_task = currentTask;
		this.total_tasks = totalTasks;
	}
	
	public void run()
	{
		try
		{
			imageuri = android.net.Uri.parse(myuri);
			imageurl = new URL(myuri);
			connection = (HttpURLConnection)imageurl.openConnection();
			connection.connect();
		}
		catch(MalformedURLException ex)
		{
			ex.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	
		//path_segments = imageuri.getPathSegments();
		file_name = imageuri.getPathSegments().get(imageuri.getPathSegments().size() - 1);
		localPath.mkdirs();
		
		try
		{
			input = new BufferedInputStream(imageurl.openStream());
			fully_qualified_file_name = new File(localPath,file_name);
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
			writeProgress(current_task, total_tasks);
			new folder_scanner(mContext,fully_qualified_file_name);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

}
