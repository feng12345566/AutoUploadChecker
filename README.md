AutoUploadChecker
=================

应用检查更新，多线程断点下载apk，可暂停、续传，下载完成后自动安装apk.


使用方法：
    1.复制lib工程至eclipse;
    2.在应用工程中引用该lib工程；
    3.调用lib中的方法：
    UpdateChecker.setUpdateServerUrl(serverUrl);
		UpdateChecker.checkForDialog(context); 
		
		或
		UpdateChecker.setUpdateServerUrl(serverUrl);
		UpdateChecker.checkForNotification(context); 
		
		serverUrl为服务端请求地址，需打印出知道格式的Json字符串，如：
		JSONObject json=new JSONObject();
    json.put("url", "http://shouji.360tpcdn.com/140807/c27d9c5fc735edd2bbca86e53abcddf1/com.tencent.mobileqq_146.apk");
    json.put("updateMessage", "1.全新界面；\n2.新增多人通话功能；\n3.优化性能，运行更流畅；\n4.修复已知bug。");
    json.put("versionCode",2);
    out.print(json.toString());
