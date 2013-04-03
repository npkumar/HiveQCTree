package com.imaginea.qctree.hive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import com.imaginea.qctree.Cell;
import com.imaginea.qctree.Property;
import com.imaginea.qctree.QCCube;
import com.imaginea.qctree.Row;
import com.imaginea.qctree.Table;
import com.imaginea.qctree.Class;
import com.imaginea.qctree.hadoop.QCTree;


public class Hivejdbc {

	private static final Log LOG = LogFactory.getLog(Hivejdbc.class);
	private static String driverName = Property.HIVE_DRIVER_NAME;
	private static Hivejdbc hivejdbc;
	
	private String wareHouseURL = new StringBuilder().append("jdbc:hive://").append(Property.HIVE_CLIENT_IP).append(":")
					.append(Property.HIVE_SERVER_PORT).append("/").append(Property.HIVE_DB).toString();
			
	private Connection connection;
	private Statement statement;
	private ResultSet resultset;
	
	private Table baseTable = Table.getTable();
	
	private int noOfDims = baseTable.getDimensionHeaders().size();
	private int noOfMeas = baseTable.getMeasureHeaders().size();
	
	private String[] dims = new String[noOfDims];
	private double[] meas = new double[noOfMeas];
	
	private Row row;
	
	public static Hivejdbc getObject(){
		
		if (hivejdbc == null)
			return new Hivejdbc();
		else
			return hivejdbc; 
	}
	
	private Hivejdbc(){
		
		LOG.info(Property.printProperties());
		
		try {
			  java.lang.Class.forName(driverName);
		    } catch (ClassNotFoundException e) {
		      e.printStackTrace();
		      System.exit(1);
		    }
		
		try {
			
			connection = DriverManager.getConnection(wareHouseURL, Property.HIVE_USERNAME, Property.HIVE_PASSWORD);
			statement = connection.createStatement();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/*
	 * Reads hive source table.
	 * Builds QCTree.
	 * Creates output table.
	 * Loads logs and serialised object.
	 * */
	public void buildQCTree(){
		
		String query = "select * from " + Property.BASE_TABLE_NAME;
		try {
			LOG.info("Running: " + query);
			resultset = statement.executeQuery(query);
			
			while(resultset.next()){
				try{
					for (int i = 0 ; i < noOfDims ; i++){
						dims[i] = resultset.getString(i+1);
					}
					
					for (int i =0 ; i < noOfMeas; i++){
						meas[i] = resultset.getDouble(noOfDims + noOfMeas + i);
					}
					
				}catch(Exception e){
					LOG.info("Invalid record!");
				}
				
				row = new Row(dims, meas);
				
				if (LOG.isInfoEnabled()) {
				       LOG.info("Adding row : " + row);
				}
				baseTable.addRow(row);
			}
			
		} catch (SQLException e) { 
			e.printStackTrace();
		}
	
		QCCube cube = QCCube.construct();
		cube.printQCLattice();
		QCTree tree = QCTree.build(cube);
		
		LOG.info("attempting a serialization on tree object..");
		doSerialize(tree);
		
		LOG.info("Creating table.. " + createTable(Property.QC_TABLE_NAME));
		
		LOG.info("Loading files.. " + loadFiles());
	}
	
	/*
	 * Serialises object to be loaded. 
	 * */
	private void doSerialize(QCTree tree) {
		FileOutputStream fos;
		ObjectOutputStream oos;
		
		try {
			fos = new FileOutputStream(Property.QCTREE_FILENAME);
			try {
				oos = new ObjectOutputStream(fos);
				oos.writeObject(tree);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}
		
	}

	/*
	 * For testing.
	 * */
	private void doLocalDeserialize() {
		String fullPath = System.getProperty("user.dir") + "/" + Property.QCTREE_FILENAME;
		try {
			FileInputStream fis = new FileInputStream(fullPath);
			try {
				ObjectInputStream ois = new ObjectInputStream(fis);
				try {
					QCTree tree = (QCTree) ois.readObject();
				} catch (ClassNotFoundException e) {

					e.printStackTrace();
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}

	/*
	 * Reads serialised object from HDFS.
	 * Returns QCTree data structure.
	 * */
	public QCTree getTree(){
		
		String uri = new StringBuilder().append("hdfs://").append(Property.HIVE_CLIENT_IP)
				.append(":").append(Property.HIVE_PORT).append(Property.WAREHOUSE_PATH).append("/")
				.append(Property.QC_TABLE_NAME).append("/").append(Property.QCTREE_FILENAME).toString();
		
		LOG.info("URI: " + uri);
		
		Configuration conf = new Configuration();
		
		FileSystem fs = null;
		InputStream in = null;
		QCTree tree = null;
		
		try {
			
			fs = FileSystem.get(URI.create(uri), conf);
			in = fs.open(new Path(uri));
			ObjectInputStream ois = new ObjectInputStream(in);
			try {
				tree = (QCTree) ois.readObject();
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
			}
			//IOUtils.copyBytes(in, System.out, 4096, false);
			
		}catch(IOException e){
			e.printStackTrace();
		} finally {
			IOUtils.closeStream(in);
		}
		
		return tree;
	}
	
	/*
	 * Creation of dummy hive table} for final output.
	 * */
	private Boolean createTable(String tablename){
		
		String query = "create table " + tablename + " (def string)";
		LOG.info("Create table: "+ query);

		try {
			resultset = statement.executeQuery(query);
		} catch (SQLException e) {
			//e.printStackTrace();
			resultset = null;
		}
		
		if (resultset == null)
			LOG.info("Did not create table, exists!");
		else
			LOG.info("Table created " + tablename);
		
		return (resultset == null ? false : true);
	}
	
	/*
	 * Initial load of files to newly created base table.
	 * */
	public void loadRawFiles(){
		String rawFileDir = System.getProperty("user.dir") + "/rawfiles";
		
		File dir = new File(rawFileDir);
		
		for (String f : dir.list()){
			try {
				resultset = statement.executeQuery("load data local inpath '" + rawFileDir+"/"+f +"' into table " + Property.BASE_TABLE_NAME );
				if(resultset != null)
					LOG.info("Loaded RawFile: " + f);
			} catch (SQLException e) {
				LOG.info("Expection loading RawFile: " + f);
				e.printStackTrace();
			}
			
		}
	}
	
	
	/*
	 * Loads lattice structure and serialised object
	 * onto the hive base table.
	 * */
	private Boolean loadFiles(){
	
		Boolean success = false;
		
		try {
			String fullPath = System.getProperty("user.dir") + "/" + Property.LATTICE_FILENAME;
			LOG.info("Lattice structure: " + fullPath);
			resultset = statement.executeQuery("load data local inpath '" + fullPath +"' into table " + Property.QC_TABLE_NAME );
			if(resultset != null){
				LOG.info("loaded qclattice data");
				new File(fullPath).delete();
				LOG.info("local qclattice file deleted.");
			}
			
			fullPath = System.getProperty("user.dir") + "/" + Property.QCTREE_FILENAME;
			LOG.info("Lattice structure: " + fullPath);
			resultset = statement.executeQuery("load data local inpath '" + fullPath +"' into table " + Property.QC_TABLE_NAME );
			if(resultset != null){
				LOG.info("loaded serialised object");
				new File(fullPath).delete();
				LOG.info("local serialised object deleted.");
			}
			
			success = true;
			
		} catch (SQLException e) {
			
			success = false;
			e.printStackTrace();
		}finally{
			try {
				resultset.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return success;
	}
	
	/*
	 * Check if source table exists. If not, create based on
	 * data provided in the JSON object.
	 * */
	public Boolean checkSource(){
		
		Boolean ifExists = false;
		
		StringBuilder sb = new StringBuilder();
		for (String s : baseTable.getDimensionHeaders()){
			sb.append(s).append(" string, ");
		}
		String dims = sb.toString();
		sb = new StringBuilder();
		for (String s : baseTable.getMeasureHeaders()){
			sb.append(s).append(" float, ");
		}
		String meas = sb.toString();
		meas = meas.substring(0, meas.lastIndexOf(","));
		
		String query = new StringBuilder().append("create table ").append(Property.BASE_TABLE_NAME)
				.append(" (").append(dims).append(meas).append(" )").append(" row format delimited fields terminated" +
						" by '"+ Property.HIVE_TABLE_FIELD_SEPERATOR +
						"' escaped by '"+ Property.HIVE_TABLE_ESCAPED_BY +"' lines terminated by '"+ 
						Property.HIVE_TABLE_LINE_TERMINATOR +"'").toString();
		
		LOG.info("Query: " + query);
		
		try {
			resultset = statement.executeQuery(query);
			ifExists = false;
			LOG.info(Property.BASE_TABLE_NAME + " does not exist, table created.");
		} catch (SQLException e) {
			ifExists = true;
			LOG.info(Property.BASE_TABLE_NAME + " exists, table not created.");
			//e.printStackTrace();
		}
		
		return ifExists;
	}
	
	/*
	 * Need to store aggregate values as well,
	 * does not work with just the lattice structure.
	 * Direction 1
	 * */
	private void buildTree(){
		Set<Class> classes = new TreeSet<Class>();
		Cell cell;
		try {
			resultset = statement.executeQuery("select * from qcube_lattice");
			while(resultset.next()){
				Class theclass = new Class();
				
				theclass.setClassID(resultset.getInt(1));
				cell = new Cell(resultset.getString(2).toString().split(" ")); 
				theclass.setUpperBound(cell);
				cell = new Cell(resultset.getString(3).toString().split(" "));
				theclass.setLowerBound(cell);
				
				int chdId = resultset.getInt(4); 
				if (chdId == -1)
					theclass.setChild(null);
				else{
					//theclass.setChild(new Class(chdId));
					for (Class c: classes)
						if (c.getClassID() == chdId) theclass.setChild(c);
				}
					
				classes.add(theclass);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		for (Class c : classes){
			System.out.println(c);
		}
		
	}
}
