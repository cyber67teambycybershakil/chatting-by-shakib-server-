package com.shakib.chatting;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class ApiServer {
 private final Store db;
 private final File uploads;
 private ServerSocket socket;
 private final ExecutorService pool=Executors.newFixedThreadPool(16);
 ApiServer(Context context){db=new Store(context);uploads=new File(context.getFilesDir(),"uploads");uploads.mkdirs();}
 public void start()throws IOException {socket=new ServerSocket();socket.bind(new InetSocketAddress("0.0.0.0",8080));Thread t=new Thread(()->{while(!socket.isClosed()){try{Socket s=socket.accept();s.setSoTimeout(12000);pool.execute(()->handle(s));}catch(IOException e){if(!socket.isClosed())e.printStackTrace();}}});t.setDaemon(true);t.start();}
 private void handle(Socket s){try(Socket ignored=s){InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();ByteArrayOutputStream h=new ByteArrayOutputStream();int x;while((x=in.read())!=-1){h.write(x);byte[] a=h.toByteArray();int n=a.length;if(n>=4&&a[n-4]=='\r'&&a[n-3]=='\n'&&a[n-2]=='\r'&&a[n-1]=='\n')break;if(n>16000)throw new Exception("Headers too large");}String header=h.toString("UTF-8");String[] lines=header.split("\r\n");if(lines.length==0)throw new Exception("Invalid request");String[] first=lines[0].split(" ");if(first.length<2)throw new Exception("Invalid request");String method=first[0],path=first[1].split("\\?")[0];int len=0;String token="";for(String line:lines){String low=line.toLowerCase(Locale.ROOT);if(low.startsWith("content-length:"))len=Integer.parseInt(line.substring(15).trim());if(low.startsWith("authorization: bearer "))token=line.substring(22).trim();}if(len<0||len>1600000)throw new Exception("Request too large (max 1.6MB)");byte[] body=new byte[len];int p=0;while(p<len){int n=in.read(body,p,len-p);if(n<0)throw new EOFException();p+=n;}JSONObject j=len==0?new JSONObject():new JSONObject(new String(body,StandardCharsets.UTF_8));String uid=db.user(token);JSONObject result=new JSONObject();int status=200;
 if(method.equals("GET")&&path.equals("/api/health")){result.put("status","ok");result.put("demo",true);}
 else if(method.equals("POST")&&path.equals("/api/auth/register")){result.put("token",db.register(j.getString("username"),j.getString("password")));}
 else if(method.equals("POST")&&path.equals("/api/auth/login")){result.put("token",db.login(j.getString("username"),j.getString("password")));}
 else if(uid==null){status=401;result.put("error","Login required");}
 else if(method.equals("POST")&&path.equals("/api/uploads")){String data=j.getString("base64");byte[] bytes=Base64.getDecoder().decode(data);if(bytes.length>1000000)throw new Exception("Image max 1MB");String type=j.optString("mime","");if(!type.equals("image/jpeg")&&!type.equals("image/png")&&!type.equals("image/webp"))throw new Exception("Only JPEG PNG WebP");if(!validImage(bytes,type))throw new Exception("Invalid image format");String id=UUID.randomUUID().toString();try(FileOutputStream f=new FileOutputStream(new File(uploads,id))){f.write(bytes);}result.put("fileId",id);result.put("warning","Demo: images accessible to logged-in users only; private ownership rules not implemented");}
 else if(method.equals("GET")&&path.startsWith("/api/files/")){String id=path.substring(11);if(!id.matches("[a-f0-9-]{36}"))throw new Exception("Invalid file id");File f=new File(uploads,id);if(!f.isFile()){respond(out,404,"application/json","{\"error\":\"Not found\"}".getBytes(StandardCharsets.UTF_8));return;}byte[] b=readFile(f);respond(out,200,"application/octet-stream",b);return;}
 else if(method.equals("POST")&&path.equals("/api/posts")){String text=j.optString("text","");String image=j.optString("fileId","");validate(text,image);result=db.create("posts",uid,null,text,image);}
 else if(method.equals("GET")&&path.equals("/api/posts")){result.put("posts",db.list("posts",uid,null));}
 else if(method.equals("POST")&&path.equals("/api/messages")){String peer=db.findUser(j.getString("receiverUsername"));if(peer==null)throw new Exception("User not found");String text=j.optString("text","");String image=j.optString("fileId","");validate(text,image);result=db.create("messages",uid,peer,text,image);}
 else if(method.equals("GET")&&path.startsWith("/api/messages/")){String peer=db.findUser(URLDecoder.decode(path.substring(14),"UTF-8"));if(peer==null)throw new Exception("User not found");result.put("messages",db.list("messages",uid,peer));}
 else {status=404;result.put("error","Unknown endpoint");}
 respond(out,status,"application/json; charset=utf-8",result.toString().getBytes(StandardCharsets.UTF_8));
 }catch(Exception e){try{respond(s.getOutputStream(),400,"application/json; charset=utf-8",new JSONObject().put("error",e.getMessage()==null?"Bad request":e.getMessage()).toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}}}
 private void validate(String text,String image)throws Exception {if(text.length()>5000)throw new Exception("Text max 5000 chars");if(text.trim().isEmpty()&&image.isEmpty())throw new Exception("Text or image required");if(!image.isEmpty()&&(!image.matches("[a-f0-9-]{36}")||!new File(uploads,image).isFile()))throw new Exception("Upload image first");}
 private boolean validImage(byte[] b,String mime){if(mime.equals("image/jpeg"))return b.length>3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;if(mime.equals("image/png"))return b.length>8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71;return b.length>12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';}
 private byte[] readFile(File f)throws IOException{try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 private void respond(OutputStream out,int code,String type,byte[] body)throws IOException {String reason=code==200?"OK":code==401?"Unauthorized":code==404?"Not Found":"Bad Request";String h="HTTP/1.1 "+code+" "+reason+"\r\nContent-Type: "+type+"\r\nContent-Length: "+body.length+"\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";out.write(h.getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();}
}
