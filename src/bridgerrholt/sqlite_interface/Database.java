package bridgerrholt.sqlite_interface;

import java.sql.*;

public class Database {
	private String     fileName;
	private Connection connection = null;


	public Database(String fileName) throws Exception {
		// Fail if JDBC is not found.
		Class.forName("org.sqlite.JDBC");

		this.fileName = "jdbc:sqlite:" + fileName;

		connection = DriverManager.getConnection(this.fileName);
	}


	public void close() {
		try {
			connection.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


	public Connection getConnection() {
		return connection;
	}


	public ResultSet select(String contents, String tableName) {
		Statement stmt = getConnection().createStatement();
		return stmt.executeQuery("");
	}
}
