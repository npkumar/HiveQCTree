package com.imaginea.qctree;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public final class Property {

	private static Properties prop;
	
	private static final String CONFIG_FILE = "config.properties";
	
	static { 
		
		prop = new Properties();
		try {
			prop.load(new FileInputStream(CONFIG_FILE));
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	
	private Property(){
		
	}
	
	public static final String BASE_TABLE_NAME = prop.getProperty("base_table_name");
	
	public static final String QC_TABLE_NAME = "qc_" + prop.getProperty("base_table_name");
	
	// Hive Properties
	public static final String WAREHOUSE_PATH = prop.getProperty("warehouse_path");
	
	public static final String HIVE_USERNAME = prop.getProperty("hive_username");
	
	public static final String HIVE_PASSWORD = prop.getProperty("hive_password");
	
	public static final String HIVE_PORT = prop.getProperty("hive_port");
	
	public static final String HIVE_SERVER_PORT = prop.getProperty("hive_server_port");
	
	public static final String HIVE_CLIENT_IP = prop.getProperty("hive_client_ip");
	
	public static final String HIVE_DB = prop.getProperty("hive_db");
	
	public static final String HIVE_DRIVER_NAME = prop.getProperty("hive_driver_name");
	
	public static final String HIVE_TABLE_LINE_TERMINATOR = prop.getProperty("hive_table_line_terminator");
			
	public static final String HIVE_TABLE_FIELD_SEPERATOR = prop.getProperty("hive_table_field_seperator");
			
	public static final String HIVE_TABLE_ESCAPED_BY = prop.getProperty("hive_table_escaped_by");		
	
	//Query
	public static final String QUERY_FILE_SEPERATOR = prop.getProperty("query_file_seperator");
	
	//QCTree
	
	public static final String LATTICE_FILENAME = prop.getProperty("lattice_filename");
	
	public static final String QCTREE_FILENAME = prop.getProperty("qctree_filename");
	
	public static String printProperties(){
		Enumeration<Object> e = prop.keys();
		StringBuilder sb = new StringBuilder();
		while(e.hasMoreElements()){
			String key = (String) e.nextElement();
			sb.append(key + " : " + prop.getProperty(key));
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
