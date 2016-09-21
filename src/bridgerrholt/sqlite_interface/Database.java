package bridgerrholt.sqlite_interface;

import java.sql.*;

public class Database {
	private String     fileName;
	private Connection connection = null;

	public Database(String fileName) throws ClassNotFoundException,
			                                    SQLException {
		this.fileName = fileName;

		// Fail if JDBC is not found.
		Class.forName("org.sqlite.JDBC");

		connection = DriverManager.getConnection(fileName);
	}
}
