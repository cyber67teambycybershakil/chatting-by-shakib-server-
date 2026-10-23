package com.shakib.chatting;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
class Security {
 static String hash(String p)throws Exception{byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);byte[] h=derive(p,salt);return Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(h);}
 static boolean verify(String p,String stored){try{String[] a=stored.split(":");return MessageDigest.isEqual(derive(p,Base64.getDecoder().decode(a[0])),Base64.getDecoder().decode(a[1]));}catch(Exception e){return false;}}
 static byte[] derive(String p,byte[] salt)throws Exception{PBEKeySpec s=new PBEKeySpec(p.toCharArray(),salt,120000,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(s).getEncoded();}finally{s.clearPassword();}}
}
