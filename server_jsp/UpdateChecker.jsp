<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
JSONObject json=new JSONObject();
json.put("url", "http://shouji.360tpcdn.com/140807/c27d9c5fc735edd2bbca86e53abcddf1/com.tencent.mobileqq_146.apk");
json.put("updateMessage", "1.全新界面；\n2.新增多人通话功能；\n3.优化性能，运行更流畅；\n4.修复已知bug。");
json.put("versionCode",2);
out.print(json.toString());
%>