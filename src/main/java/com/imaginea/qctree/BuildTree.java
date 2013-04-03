package com.imaginea.qctree;

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.imaginea.qctree.hive.Hivejdbc;

public class BuildTree {

	private static final Log LOG = LogFactory.getLog(BuildTree.class);
	
	/*
	 * Check for HIVE_SERVER_PORT usage.
	 * */
	private static Boolean isRunning(int port){
		ServerSocket sc = null;
		Boolean running = false;
		
		try {
			sc = new ServerSocket(port);
			running = false;
		} catch (IOException e) {
			running = true;
		}finally{
			try {
				if (sc != null)
					sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return running;
	}
	
	
	public static void main (String[] args ){
		
		if (isRunning(Integer.parseInt(Property.HIVE_SERVER_PORT))){
			
			LOG.info("Hive Server running at: " + Property.HIVE_SERVER_PORT);
			
			Hivejdbc obj = Hivejdbc.getObject();
			
			// Proceed based on availability of base table.
			if(obj.checkSource())
				// Build cube from existing hive table.
				obj.buildQCTree();
			else{
				// Load files and build table.
				obj.loadRawFiles();
				obj.buildQCTree();
			}
		}else{
			LOG.info("Exiting. HiveServer not running at:" + Property.HIVE_SERVER_PORT );
			System.exit(1);
		}
		
		
	}
}
