package com.yyxu.download.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.loveplusplus.update.R;
import com.loveplusplus.update.UpdateChecker;
import com.yyxu.download.utils.ConfigUtils;
import com.yyxu.download.utils.MyIntents;
import com.yyxu.download.utils.NetworkUtils;
import com.yyxu.download.utils.StorageUtils;

public class DownloadManager extends Thread {

	private static final int MAX_TASK_COUNT = 100;
	private static final int MAX_DOWNLOAD_THREAD_COUNT = 3;
	private static final String TAG = "DownloadService";
	private NotificationManager mNotifyManager;
	private android.support.v4.app.NotificationCompat.Builder mBuilder;

	private Context mContext;

	private TaskQueue mTaskQueue;
	private List<DownloadTask> mDownloadingTasks;
	private List<DownloadTask> mPausingTasks;

	private Boolean isRunning = false;

	public DownloadManager(Context context) {

		mContext = context;
		mTaskQueue = new TaskQueue();
		mDownloadingTasks = new ArrayList<DownloadTask>();
		mPausingTasks = new ArrayList<DownloadTask>();
		mNotifyManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(mContext);

		String appName = mContext
				.getString(mContext.getApplicationInfo().labelRes);
		int icon = mContext.getApplicationInfo().icon;

		mBuilder.setContentTitle(appName).setSmallIcon(icon);
	}

	public void startManage() {

		isRunning = true;
		this.start();
		checkUncompleteTasks();
	}

	public void close() {

		isRunning = false;
		// pauseAllTask();
		this.stop();
	}

	public boolean isRunning() {

		return isRunning;
	}

	@Override
	public void run() {

		super.run();
		while (isRunning) {
			DownloadTask task = mTaskQueue.poll();
			mDownloadingTasks.add(task);
			task.execute();
		}
	}

	public void addTask(String url) {

		if (!StorageUtils.isSDCardPresent()) {
			Toast.makeText(mContext, "未发现SD卡", Toast.LENGTH_LONG).show();
			return;
		}

		if (!StorageUtils.isSdCardWrittenable()) {
			Toast.makeText(mContext, "SD卡不能读写", Toast.LENGTH_LONG).show();
			return;
		}

		if (getTotalTaskCount() >= MAX_TASK_COUNT) {
			Toast.makeText(mContext, "任务列表已满", Toast.LENGTH_LONG).show();
			return;
		}
		File file = new File(StorageUtils.getCacheDirectory(mContext),
				NetworkUtils.getFileNameFromUrl(url));
		if (file.exists()) {
			PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
					0, StorageUtils.getInstallAPKIntent(mContext, url),
					PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(pendingIntent);
			mBuilder.setContentText("点击安装");
			Notification noti = mBuilder.build();
			noti.flags = android.app.Notification.FLAG_AUTO_CANCEL;
			mNotifyManager.notify(0, noti);
		} else {
			try {

				if (!hasTask(url)) {
					addTask(newDownloadTask(url));
					System.out.println("添加" + url);
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

	}

	private void addTask(DownloadTask task) {

		// broadcastAddTask(task.getUrl());

		mTaskQueue.offer(task);

		if (!this.isAlive()) {
			this.startManage();
		}
	}

	public boolean hasTask(String url) {

		DownloadTask task;
		for (int i = 0; i < mDownloadingTasks.size(); i++) {
			task = mDownloadingTasks.get(i);
			if (task.getUrl().equals(url)) {
				return true;
			}
		}
		for (int i = 0; i < mTaskQueue.size(); i++) {
			task = mTaskQueue.get(i);
		}
		return false;
	}

	public DownloadTask getTask(int position) {

		if (position >= mDownloadingTasks.size()) {
			return mTaskQueue.get(position - mDownloadingTasks.size());
		} else {
			return mDownloadingTasks.get(position);
		}
	}

	public int getQueueTaskCount() {

		return mTaskQueue.size();
	}

	public int getDownloadingTaskCount() {

		return mDownloadingTasks.size();
	}

	public int getPausingTaskCount() {

		return mPausingTasks.size();
	}

	public int getTotalTaskCount() {

		return getQueueTaskCount() + getDownloadingTaskCount()
				+ getPausingTaskCount();
	}

	public void checkUncompleteTasks() {

		List<String> urlList = ConfigUtils.getURLArray(mContext);
		if (urlList.size() >= 0) {
			for (int i = 0; i < urlList.size(); i++) {
				addTask(urlList.get(i));
			}
		}
	}

	public synchronized void pauseTask(String url) {

		DownloadTask task;
		
		for (int i = 0; i < mDownloadingTasks.size(); i++) {
			task = mDownloadingTasks.get(i);
			if (task != null && task.getUrl().equals(url)) {
				pauseTask(task);
				Intent intent = new Intent(mContext, DownloadService.class);
				intent.putExtra(MyIntents.TYPE, MyIntents.Types.CONTINUE);
				intent.putExtra(MyIntents.URL, url);
				mBuilder.setContentText(
						"已暂停，点击继续下载")
						.setProgress(100, task.getOldProcess(), false);
				// setContentInent如果不设置在4.0+上没有问题，在4.0以下会报异常
				PendingIntent pendingintent = PendingIntent.getService(mContext, 0,
						intent, PendingIntent.FLAG_CANCEL_CURRENT);
				mBuilder.setContentIntent(pendingintent);
				mNotifyManager.notify(0, mBuilder.build());
			}
		}
	}

	// public synchronized void pauseAllTask() {
	//
	// DownloadTask task;
	//
	// for (int i = 0; i < mTaskQueue.size(); i++) {
	// task = mTaskQueue.get(i);
	// mTaskQueue.remove(task);
	// mPausingTasks.add(task);
	// }
	//
	// for (int i = 0; i < mDownloadingTasks.size(); i++) {
	// task = mDownloadingTasks.get(i);
	// if (task != null) {
	// pauseTask(task);
	// }
	// }
	// }

//	/**
//	 * 删除下载任务
//	 * 
//	 * @param url
//	 */
//	public synchronized void deleteTask(String url) {
//
//		DownloadTask task;
//		for (int i = 0; i < mDownloadingTasks.size(); i++) {
//			task = mDownloadingTasks.get(i);
//			if (task != null && task.getUrl().equals(url)) {
//				File file = new File(StorageUtils.getCacheDirectory(mContext)
//						.getAbsoluteFile()
//						+ "/"
//						+ NetworkUtils.getFileNameFromUrl(task.getUrl()));
//				if (file.exists())
//					file.delete();
//
//				task.onCancelled();
//				completeTask(task);
//				return;
//			}
//		}
//		for (int i = 0; i < mTaskQueue.size(); i++) {
//			task = mTaskQueue.get(i);
//			if (task != null && task.getUrl().equals(url)) {
//				mTaskQueue.remove(task);
//			}
//		}
//		for (int i = 0; i < mPausingTasks.size(); i++) {
//			task = mPausingTasks.get(i);
//			if (task != null && task.getUrl().equals(url)) {
//				mPausingTasks.remove(task);
//			}
//		}
//	}

	/**
	 * 继续下载任务
	 * 
	 * @param url
	 */
	public synchronized void continueTask(String url) {

		DownloadTask task;
		
		for (int i = 0; i < mPausingTasks.size(); i++) {
			task = mPausingTasks.get(i);
			if (task != null && task.getUrl().equals(url)) {
				continueTask(task);
				Intent intent = new Intent(mContext, DownloadService.class);
				intent.putExtra(MyIntents.TYPE, MyIntents.Types.PAUSE);
				intent.putExtra(MyIntents.URL, url);
				updateProgress(task.getOldProcess(), intent);
			}

		}
	}

	/**
	 * 暂停下载任务
	 * 
	 * @param task
	 */
	public synchronized void pauseTask(DownloadTask task) {

		if (task != null) {
			task.onCancelled();

			// move to pausing list
			String url = task.getUrl();
			try {
				mDownloadingTasks.remove(task);
				task = newDownloadTask(url);
				mPausingTasks.add(task);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}

		}
	}

	public synchronized void continueTask(DownloadTask task) {

		if (task != null) {
			mPausingTasks.remove(task);
			mTaskQueue.offer(task);
		}
	}

	public synchronized void completeTask(DownloadTask task) {

		for (int i = 0; i < mDownloadingTasks.size(); i++) {
			if (task.getUrl().endsWith(mDownloadingTasks.get(i).getUrl())) {
				ConfigUtils.clearURL(mContext, i);
				mDownloadingTasks.remove(task);
                Intent intent=StorageUtils.getInstallAPKIntent(mContext, task.getUrl());
                mContext.startActivity(intent);
				Notification noti = mBuilder.build();
				noti.flags = android.app.Notification.FLAG_AUTO_CANCEL;
				mNotifyManager.cancel(0);

			}
		}
	}

	private void updateProgress(int progress, Intent intent) {
		// "正在下载:" + progress + "%"
		mBuilder.setContentText(
				mContext.getString(R.string.download_progress, progress))
				.setProgress(100, progress, false);
		// setContentInent如果不设置在4.0+上没有问题，在4.0以下会报异常
		PendingIntent pendingintent = PendingIntent.getService(mContext, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		mBuilder.setContentIntent(pendingintent);
		mNotifyManager.notify(0, mBuilder.build());
	}

	/**
	 * Create a new download task with default config
	 * 
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	private DownloadTask newDownloadTask(String url)
			throws MalformedURLException {

		DownloadTaskListener taskListener = new DownloadTaskListener() {

			@Override
			public void updateProcess(DownloadTask task) {
				Intent intent = new Intent(mContext, DownloadService.class);
				intent.putExtra(MyIntents.TYPE, MyIntents.Types.PAUSE);
				intent.putExtra(MyIntents.URL, task.getUrl());

				int progress = (int) ((task.getDownloadSize() * 1.0 / task
						.getTotalSize()) * 100);
				if (progress != 0 && task.getOldProcess() != progress) {
					task.setOldProcess(progress);
					updateProgress(progress, intent);
					Log.e("process", progress + "%");
				}

			}

			@Override
			public void preDownload(DownloadTask task) {

				ConfigUtils.storeURL(mContext, mDownloadingTasks.indexOf(task),
						task.getUrl());
				updateProgress(0, new Intent());
			}

			@Override
			public void finishDownload(DownloadTask task) {
				completeTask(task);
			}

			@Override
			public void errorDownload(DownloadTask task, Throwable error) {

				if (error != null) {
					Toast.makeText(mContext, "Error: " + error.getMessage(),
							Toast.LENGTH_LONG).show();
				}

				// Intent errorIntent = new
				// Intent("com.yyxu.download.activities.DownloadListActivity");
				// errorIntent.putExtra(MyIntents.TYPE, MyIntents.Types.ERROR);
				// errorIntent.putExtra(MyIntents.ERROR_CODE, error);
				// errorIntent.putExtra(MyIntents.ERROR_INFO,
				// DownloadTask.getErrorInfo(error));
				// errorIntent.putExtra(MyIntents.URL, task.getUrl());
				// mContext.sendBroadcast(errorIntent);
				//
				// if (error != DownloadTask.ERROR_UNKOWN_HOST
				// && error != DownloadTask.ERROR_BLOCK_INTERNET
				// && error != DownloadTask.ERROR_TIME_OUT) {
				// completeTask(task);
				// }
			}
		};
		return new DownloadTask(mContext, url, StorageUtils.getCacheDirectory(
				mContext).getAbsolutePath(), taskListener);
	}

	/**
	 * A obstructed task queue
	 * 
	 * @author Yingyi Xu
	 */
	private class TaskQueue {
		private Queue<DownloadTask> taskQueue;

		public TaskQueue() {

			taskQueue = new LinkedList<DownloadTask>();
		}

		public void offer(DownloadTask task) {

			taskQueue.offer(task);
		}

		public DownloadTask poll() {

			DownloadTask task = null;
			while (mDownloadingTasks.size() >= MAX_DOWNLOAD_THREAD_COUNT
					|| (task = taskQueue.poll()) == null) {
				try {
					Thread.sleep(1000); // sleep
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return task;
		}

		public DownloadTask get(int position) {

			if (position >= size()) {
				return null;
			}
			return ((LinkedList<DownloadTask>) taskQueue).get(position);
		}

		public int size() {

			return taskQueue.size();
		}

		@SuppressWarnings("unused")
		public boolean remove(int position) {

			return taskQueue.remove(get(position));
		}

		public boolean remove(DownloadTask task) {

			return taskQueue.remove(task);
		}
	}

}
