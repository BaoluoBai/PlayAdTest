package com.example.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Util {
	static List<String> fileTempList = new ArrayList<String>();
	static List<String> pictureTempList = new ArrayList<String>();
	public static List<String> doSearchAd(String path) {		
		File file = new File(path);
		if (file.exists()) {	
			if (file.isDirectory()) {	
				File[] fileArray = file.listFiles();
				if(fileArray == null){
					
				}else{
					for (File f : fileArray) {
						if (f.isDirectory()) {
							doSearchAd(f.getPath());
						} 
						else {
							if(f.getName().endsWith(".mp4") || f.getName().endsWith(".avi")
									|| f.getName().endsWith(".jpg") || f.getName().endsWith(".png")){
								fileTempList.add(f.getAbsolutePath());
//								LogUtil.d(TAG, "list: "+ fileTempList.toString());
							}
						} 					
					}	
				}						
			} 			
		}
		return fileTempList;
	}
	
	public int checkXOR(byte[] buffer){
  		int vc = 0x00;
  		if(buffer.length == 1){
  			vc ^= (buffer[0]&0xFF);
  		}else{
  			for(int i=0;i<buffer.length-2;i++){
  	  			if(i==0){
  	  				vc = (buffer[i]&0xFF)^(buffer[i+1]&0xFF);
  	  			}else{
  	  				vc ^= (buffer[i+1]&0xFF);
  	  			}
  	  		}
  		}
		return (vc&0xFF);
  		
  	}
	
	public int checkADD(byte[] buffer){
		int vc = 0x00;
		if(buffer.length == 1){
  			vc &= (buffer[0]&0xFF);
  		}else{
  			for(int i=0;i<buffer.length-2;i++){
  	  			if(i==0){
  	  				vc = (buffer[i]&0xFF)&(buffer[i+1]&0xFF);
  	  			}else{
  	  				vc &= (buffer[i+1]&0xFF);
  	  			}
  	  		}
  		}
		return (vc&0xFF);
	}
}
