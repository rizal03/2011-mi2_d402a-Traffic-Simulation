package utils;

import java.sql.*;

import uk.me.jstott.jcoord.LatLng;

import dataStructures.GPSSignal;
import dataStructures.Trip;

/*
 * Every action/method related to the database, should be here.
 */
public class DatabaseUtil {

	java.sql.Connection connection;
	
	static private String defaultUrl = "jdbc:postgresql://localhost:5432/project";
	static private String defaultUser = "postgres";
	static private String defaultPassword = "123";
	
	public DatabaseUtil(){
		try{
			connection = DriverManager.getConnection(defaultUrl, defaultUser, defaultPassword);
		}
		catch( Exception e){
			e.printStackTrace();
		}	
	}
	
	public DatabaseUtil(String url, String user, String password){
		try{
			connection = DriverManager.getConnection(url, user, password);
			
		}
		catch( Exception e){
			e.printStackTrace();
		}	
	}
	
	public ResultSet query(String query){
		Statement statement = null;
		ResultSet result = null;
		try {
			statement = connection.createStatement();
			result = statement.executeQuery(query);
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		return result;
	}	
	
	
	
	public void closeConnection(){
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Finds the shortest path between two points and return the Trip
	 */
	public Trip getShortestPath(GPSSignal from, GPSSignal to) throws SQLException{
		int idFrom = getClosestPoint(from);
		int idTo = getClosestPoint(to);
			
		String sql = "SELECT * FROM shortest_path(' " +
				"SELECT gid AS id," +
				"start_id::int4 AS source," +
				"end_id::int4 AS target," +
				"ST_Length(the_geom)::float8 AS cost" +
				"FROM network', "+idFrom+", "+idTo+", false, false);";
		
		Statement statement = this.connection.createStatement();
		ResultSet result = statement.executeQuery(sql);
		return Utils.ResultSet2Trip(result);
	}
	
	private Integer getClosestPoint(GPSSignal signal) throws SQLException{		
		if(signal.getFormat() != "UML")
			signal = Utils.UTM2GWS84(signal);
			
		String sql = "SELECT id" +
					"FROM network as f" +
					"WHERE EXISTS (" +
						"SELECT min(ST_Distance(ST_ClosestPoint(g.the_geom, pt), pt) )" +
						"FROM (" +
						"SELECT" +
							"'POINT("+signal.getLatitude()+" "+signal.getLongitude()+")'::geometry AS pt" +
						") AS foo" +
					")" +
					"LIMIT 1";
		
		int id = -1;
		Statement statement = this.connection.createStatement();
		ResultSet result = statement.executeQuery(sql);
		id = Integer.parseInt(result.getString(0));
		result.close();
		statement.close();		
		return id;
	}
}
