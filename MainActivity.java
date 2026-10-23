package com.shakib.chatting;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.net.*;

public class MainActivity extends Activity {
 static ApiServer server;
 TextView info;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  LinearLayout l=new LinearLayout(this); l.setPadding(28,40,28,20); l.setOrientation(1);
  TextView title=new TextView(this);title.setText("Chatting by Shakib — LAN Demo Server");title.setTextSize(23);l.addView(title);
  info=new TextView(this);l.addView(info);
  Button start=new Button(this);start.setText("Start server");l.addView(start);
  start.setOnClickListener(v->{try{if(server==null){server=new ApiServer(this);server.start();}info.setText("Server running. On same Wi-Fi open: http://"+ip()+":8080\nKeep this app open. Demo only; not public internet safe.");}catch(Exception e){info.setText("Error: "+e.getMessage());}});
  setContentView(l);info.setText("Tap Start server. Connect another device on the same Wi-Fi.");
 }
 String ip(){try{java.util.Enumeration<NetworkInterface> es=NetworkInterface.getNetworkInterfaces();while(es.hasMoreElements()){NetworkInterface n=es.nextElement();if(!n.isUp()||n.isLoopback())continue;java.util.Enumeration<InetAddress> a=n.getInetAddresses();while(a.hasMoreElements()){InetAddress x=a.nextElement();if(x instanceof Inet4Address&&!x.isLoopbackAddress()&&x.isSiteLocalAddress())return x.getHostAddress();}}}catch(Exception ignored){}return "PHONE_IP";}
}
