package com.shakib.chatting;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

class Store extends SQLiteOpenHelper {
 Store(Context c){super(c,"shakib.db",null,1);}
 public void onCreate(SQLiteDatabase d){
  d.execSQL("CREATE TABLE users(id TEXT PRIMARY KEY, name TEXT UNIQUE NOT NULL, pass TEXT NOT NULL)");
  d.execSQL("CREATE TABLE sessions(token TEXT PRIMARY KEY, uid TEXT NOT NULL)");
  d.execSQL("CREATE TABLE posts(id TEXT PRIMARY KEY, uid TEXT, body TEXT, image TEXT, created INTEGER)");
  d.execSQL("CREATE TABLE messages(id TEXT PRIMARY KEY, sender TEXT, receiver TEXT, body TEXT, image TEXT, created INTEGER, expires INTEGER)");
 }
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 synchronized String register(String name,String pass)throws Exception{if(name.length()<3||name.length()>30||!name.matches("[a-zA-Z0-9_]+")||pass.length()<8)throw new Exception("Username 3-30 letters/numbers; password 8+ characters");String uid=UUID.randomUUID().toString();android.content.ContentValues v=new android.content.ContentValues();v.put("id",uid);v.put("name",name);v.put("pass",Security.hash(pass));if(getWritableDatabase().insert("users",null,v)<0)throw new Exception("Username already used");return login(name,pass);}
 synchronized String login(String name,String pass)throws Exception{Cursor c=getReadableDatabase().rawQuery("SELECT id,pass FROM users WHERE name=?",new String[]{name});String uid=null;try{if(c.moveToFirst()&&Security.verify(pass,c.getString(1)))uid=c.getString(0);}finally{c.close();}if(uid==null)throw new Exception("Invalid login");String token=UUID.randomUUID().toString()+UUID.randomUUID();ContentValues v=new ContentValues();v.put("token",token);v.put("uid",uid);getWritableDatabase().insertOrThrow("sessions",null,v);return token;}
 synchronized String user(String token){Cursor c=getReadableDatabase().rawQuery("SELECT uid FROM sessions WHERE token=?",new String[]{token});try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}}
 synchronized String username(String uid){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM users WHERE id=?",new String[]{uid});try{return c.moveToFirst()?c.getString(0):"unknown";}finally{c.close();}}
 synchronized String findUser(String name){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM users WHERE name=?",new String[]{name});try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}}
 synchronized JSONObject create(String table,String uid,String receiver,String body,String image)throws Exception{
  long now=System.currentTimeMillis();String id=UUID.randomUUID().toString();ContentValues v=new ContentValues();v.put("id",id);v.put(table.equals("posts")?"uid":"sender",uid);if(table.equals("messages")){v.put("receiver",receiver);v.put("expires",now+86400000L);}v.put("body",body);v.put("image",image);v.put("created",now);getWritableDatabase().insertOrThrow(table,null,v);JSONObject j=new JSONObject();j.put("id",id);j.put("createdAt",now);if(table.equals("messages"))j.put("expiresAt",now+86400000L);return j;
 }
 synchronized JSONArray list(String table,String uid,String peer)throws Exception{
  cleanup();String sql=table.equals("posts")?"SELECT p.id,u.name,p.body,p.image,p.created FROM posts p JOIN users u ON u.id=p.uid ORDER BY p.created DESC LIMIT 100":"SELECT m.id,u.name,m.body,m.image,m.created FROM messages m JOIN users u ON u.id=m.sender WHERE ((m.sender=? AND m.receiver=?) OR (m.sender=? AND m.receiver=?)) AND m.expires>? ORDER BY m.created DESC LIMIT 100";
  Cursor c=table.equals("posts")?getReadableDatabase().rawQuery(sql,null):getReadableDatabase().rawQuery(sql,new String[]{uid,peer,peer,uid,String.valueOf(System.currentTimeMillis())});JSONArray arr=new JSONArray();try{while(c.moveToNext()){JSONObject j=new JSONObject();j.put("id",c.getString(0));j.put("from",c.getString(1));j.put("text",c.getString(2));j.put("image",c.getString(3));j.put("createdAt",c.getLong(4));arr.put(j);}}finally{c.close();}return arr;
 }
 synchronized void cleanup(){getWritableDatabase().delete("messages","expires<=?",new String[]{String.valueOf(System.currentTimeMillis())});}
}
