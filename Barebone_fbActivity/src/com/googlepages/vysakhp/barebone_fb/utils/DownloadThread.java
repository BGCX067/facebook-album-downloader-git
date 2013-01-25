package com.googlepages.vysakhp.barebone_fb.utils;

import com.googlepages.vysakhp.barebone_fb.BuildConfig;
import com.googlepages.vysakhp.barebone_fb.utils.DownloadTask;
import com.googlepages.vysakhp.barebone_fb.utils.DownloadThreadListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

//courtsey http://mindtherobot.com/blog/159/android-guts-intro-to-loopers-and-handlers/
public class DownloadThread extends Thread
{
	private Handler handler;
	private int totalQueued;
	private int totalCompleted;
	private static final String TAG = DownloadThread.class.getSimpleName();
	private DownloadThreadListener listener;
	//	
//	public void setPath(File path)
//	{
//		this.path = path;
//	
//	}
//	
//	public void setRunningTask(int RunningTask)
//	{
//		this.RunningTask = RunningTask;
//	}
//	
//	public void setWholeTasks(int WholeTasks)
//	{
//		this.WholeTasks = WholeTasks;
//	}
	public DownloadThread(DownloadThreadListener listener)
	{
		this.listener = listener;
	}
	
	public void run()
	{
		try
		{
			// preparing a looper on current thread			
			// the current thread is being detected implicitly
			Looper.prepare();
			
			if(BuildConfig.DEBUG)
			Log.i(TAG, "DownloadThread entering the loop");

			// now, the handler will automatically bind to the
			// Looper that is attached to the current thread
			// You don't need to specify the Looper explicitly
			handler = new Handler();
			
			// After the following line the thread will start
			// running the message loop and will not normally
			// exit the loop unless a problem happens or you
			// quit() the looper (see below)
			Looper.loop();
			
			if(BuildConfig.DEBUG)
			Log.i(TAG, "DownloadThread exiting gracefully");
		}
		catch (Throwable t)
		{
			Log.e(TAG, "DownloadThread halted due to an error", t);
		} 
	}
	
	// This method is allowed to be called from any thread
	public synchronized void requestStop()
	{
		// using the handler, post a Runnable that will quit()
		// the Looper attached to our DownloadThread
		// obviously, all previously queued tasks will be executed
		// before the loop gets the quit Runnable
		handler.postAtFrontOfQueue(new Runnable()
		{
			@Override
			public void run()
			{
				// This is guaranteed to run on the DownloadThread
				// so we can use myLooper() to get its looper
				if(BuildConfig.DEBUG)
				Log.i(TAG, "DownloadThread loop quitting by request");
					
				Looper.myLooper().quit();
			}
		});
	}
	
	
	
	public synchronized void enqueueDownload(final DownloadTask task)
	{
		// Wrap DownloadTask into another Runnable to track the statistics
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
//					task.setPath(path);
//					task.setCurrentTask(RunningTask);
//					task.setTotalTasks(WholeTasks);
					task.run();
					
				}
				finally
				{					
					// register task completion
					synchronized (DownloadThread.this)
					{
						totalCompleted++;
					}
					// tell the listener something has happened
					signalUpdate();
				}				
			}
		});
		
		totalQueued++;
		
		// tell the listeners the queue is now longer
		signalUpdate();
	}
	
	public synchronized int getTotalQueued()
	{
		return totalQueued;
	}
	
	public synchronized int getTotalCompleted()
	{
		return totalCompleted;
	}
	
	// Please note! This method will normally be called from the download thread.
	// Thus, it is up for the listener to deal with that (in case it is a UI component,
	// it has to execute the signal handling code in the UI thread using Handler - see
	// DownloadQueueActivity for example).
	private void signalUpdate()
	{
		if (listener != null) 
			listener.handleDownloadThreadUpdate();
	}
}

