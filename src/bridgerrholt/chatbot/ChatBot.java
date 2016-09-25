package bridgerrholt.chatbot;

import bridgerrholt.sqlite_interface.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.*;

import java.io.*;
import java.sql.*;

import java.util.*;

class StatementTables {
	String tree           = "statement_tree",
			 	 text           = "statement_text",
			 	 data           = "statement_data",
				 textSimplified = "statement_text_simplified";
}

public class ChatBot
{
	// Public
	public static void createAndRun(String databaseFile) throws Exception {
		ChatBot chatBot = new ChatBot();
		chatBot.run(databaseFile);
	}

	public ChatBot() {
		this(System.currentTimeMillis());
	}

	public ChatBot(long seed) {
		random = new Random(seed);
	}


	public void run(String databaseFile) throws Exception {
		try {
			database = Database.createConnection(databaseFile);
			//database.setAutoCommit(false);

			setupTables();

			greeting();
		}

		finally {
			if (database != null) database.close();
		}

		/*Statement statement = null;
		PreparedStatement preparedStatement = null;

		try {
			database.setAutoCommit(false);

			statement = database.createStatement();
			ResultSet introCountSet = statement.executeQuery("SELECT Count(*) FROM conversation_paths WHERE type=1");
			ResultSet introSet      = statement.executeQuery("SELECT * FROM conversation_paths WHERE type=1");

			int introCount = 0;
			while (introCountSet.next()) {
				introCount = introCountSet.getInt(1);
			}

			int access = 0;
			if (introCount == 0) {
				addRow(introCountSet, "Hey", 1);
			}
			else {
				access = random.nextInt(introCount);
			}

			introSet.absolute(access);
			prompt(introSet.getString);


		}

		finally {
			if (database != null) database.close();
		}*/
	}

	public void setInputStream(InputStream in) {
		this.in = in;
	}

	public void setOutputStream(OutputStream out) {
		this.out = new PrintStream(out);
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}


	// Private
	private Connection  database = null;
	private Random      random;

	private Locale      locale   = Locale.getDefault();
	private InputStream in       = System.in;
	private PrintStream out      = System.out;

	private static StatementTables statementTables = new StatementTables();

	private Integer position = null;
	private String  botName = "Bot";


	@FunctionalInterface
	private interface CheckedFunction<T> {
		void apply(T t) throws Exception;
	}

	private class Location
	{
		public Location(String tableName, int rowId) {
			this.tableName = tableName;
			this.rowId     = rowId;
		}

		public String getTableName() { return tableName; }
		public int    getRowId()     { return rowId; }

		private String tableName;
		private int    rowId;
	}


	private void setupTables() throws Exception {
		assert(database != null);

		try (Statement statement = database.createStatement()) {
			// Read the provided JSON file that describes all the table creation
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new FileReader("resources/table_layouts.json"));
			JsonElement json = gson.fromJson(reader, JsonElement.class);
			JsonObject root = json.getAsJsonObject();

			// Loop through each object.
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				String    tableName = entry.getKey();
				JsonArray element   = entry.getValue().getAsJsonArray();

				// Query for if the table exists.
				ResultSet result = statement.executeQuery(
					"SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "';"
				);

				// The table does not exist, so create it.
				if (!result.next()) {
					ArrayList<String> itemNames = new ArrayList<>();
					for (JsonElement item : element) {
						itemNames.add(item.getAsString());
					}

					String toExecute = "CREATE TABLE " + tableName + parenthesize(itemNames);

					statement.executeUpdate(toExecute);
				}
			}

			//database.commit();
		}
	}

	private void greeting() throws Exception {
		for (int i = 0; true; ++i) {
			ResultSet greetings = pullRows(statementTables.data, "WHERE type=?",
				statement -> {
					statement.setInt(1, 2);
				});

			if (greetings == null) {
				StatementData data = new StatementData(
					requestUserStatement("Say hello to " + botName), StatementData.Type.GREETING
				);
				addStatement(data);
			}
			else {
				CountedSet.GradedSet graded = new CountedSet(greetings).grade();
				assert graded.hasValues();

				int best = graded.getBestId();

				ResultSet greeting = pullRows(statementTables.tree, "WHERE rowid=?",
					statement -> {
						statement.setInt(1, best);
					});

				outputStatement(greeting);

				// This means the bot already had a greeting.
				if (i == 0) {
					addStatement(new StatementData(getInput(), StatementData.Type.GREETING));
				}

				break;
			}
		}
	}

	private void outputStatement(ResultSet set) throws Exception {
		setPosition(set.getInt("rowid"));

		ResultSet text = pullRows(statementTables.text, "WHERE rowid=?",
			statement -> {
				statement.setInt(1, set.getInt("text_id"));
			});

		assert (text != null);

		out.println(botName + ": " + text.getString("text"));
	}

	private void addRow(ResultSet resultSet, String textDisplay, int type) throws SQLException {
		/*resultSet.moveToInsertRow();
		resultSet.updateInt("list_id", 0);
		resultSet.updateInt("next_list_id", 0);
		resultSet.updateInt("type", type);
		resultSet.updateString("text_display", textDisplay);
		resultSet.updateString("text_simple", simplifyText(textDisplay));
		resultSet.insertRow();*/
	}

	// If the statement already exist, adds it.
	private void addStatement(StatementData data) throws Exception {
		ResultSet results = pullRows(statementTables.textSimplified,
			"WHERE text=?",
			statement -> {
				statement.setString(1, data.getTextSimplified());
			});

		int newPosition;

		if (results == null) {
			newPosition = fullStatementAdd(data);
		}
		else {
			results.next();
			final int textSimplifiedId = results.getInt("rowid");
			results = pullRows(statementTables.data,
				"WHERE text_simplified_id=? AND type=? AND has_question=?",
				statement -> {
					statement.setInt(1, textSimplifiedId);
					statement.setInt(2, data.getType().getValue());
					statement.setBoolean(3, data.hasQuestion());
				});

			if (results == null) {
				newPosition = connectToTextSimplified(data, textSimplifiedId);
			}
			else {
				results.next();
				final int dataId = results.getInt("rowid");
				results = pullRows(statementTables.text,
					"WHERE data_id=? AND text=?",
					statement -> {
						statement.setInt(1, dataId);
						statement.setString(2, data.getText());
					});

				if (results == null) {
					newPosition = connectToData(data, dataId);
				}
				else {
					results.next();
					final int textId = results.getInt("rowid");
					results = pullRows(statementTables.tree,
						"WHERE text_id=?",
						statement -> {
							statement.setInt(1, textId);
						});

					if (results == null) {
						newPosition = connectToText(data, textId);
					}
					else {
						results.next();
						newPosition = results.getInt("rowid");
						incrementCount(new Location(statementTables.tree, newPosition));
					}
				}
			}
		}

		setPosition(newPosition);
	}



	/// @return null if resultSet was empty.
	private ResultSet pullRows(String                             tableName,
	                           String                             endingClauses,
	                           CheckedFunction<PreparedStatement> binding) throws Exception {
		PreparedStatement statement = database.prepareStatement(
			"SELECT rowid, * FROM " + tableName + " " + endingClauses + ";"
		);

		binding.apply(statement);

		ResultSet returnResults = statement.executeQuery();

		if (returnResults.isBeforeFirst())
			return returnResults;

		else
			return null;
	}

	private ResultSet pullRow(Location location) throws Exception {
		return pullRows(statementTables.tree, "WHERE rowid=?",
			statement -> {
				statement.setInt(1, location.rowId);
			});
	}

	private void simpleChange(String tableName, String front, String back) throws Exception {
		Statement statement = database.createStatement();
		statement.executeUpdate(front + " " + tableName + " " + back);
	}

	private void generalChange(String                             tableName,
	                           String                             front,
	                           String                             back,
	                           CheckedFunction<PreparedStatement> binding) throws Exception {
		PreparedStatement statement = database.prepareStatement(
			front + " " + tableName + " " + back
		);

		binding.apply(statement);

		statement.executeUpdate();
	}

	private void insertTo(String                             tableName,
	                      ArrayList<String>                  columnNames,
	                      CheckedFunction<PreparedStatement> binding) throws Exception {
		ArrayList<String> insertFields = new ArrayList<>();
		for (int i = 0, size = columnNames.size(); i < size; ++i) {
			insertFields.add("?");
		}

		/*PreparedStatement statement = database.prepareStatement(
			"INSERT INTO " + tableName + " " + parenthesize(columnNames) +
			" VALUES " + parenthesize(insertFields)
		);

		binding.apply(statement);

		statement.executeUpdate();*/

		generalChange(tableName, "INSERT INTO",
			parenthesize(columnNames) + " VALUES " + parenthesize(insertFields),
			binding);
	}



	private String requestUserStatement(String output) {
		out.println(" ~ " + output);

		return getInput();
	}

	private String getInput() {
		out.print("> ");
		return new Scanner(in).nextLine();
	}


	// Returns the "rowid" value of the inserted tree row.
	private int fullStatementAdd(StatementData data) throws Exception {
		ArrayList<String> columns = new ArrayList<>();
		columns.add("text");
		insertTo(statementTables.textSimplified, columns,
			statement -> {
				statement.setString(1, data.getTextSimplified());
			});

		return connectToTextSimplified(data, lastRowId());
	}

	// Returns the "rowid" value of the inserted tree row.
	private int connectToTextSimplified(StatementData data,
	                                    int           textSimplifiedId) throws Exception {
		ArrayList<String> columns = new ArrayList<>();
		columns.add("text_simplified_id");
		columns.add("type");
		columns.add("has_question");

		insertTo(statementTables.data, columns,
			statement -> {
				statement.setInt(1, textSimplifiedId);
				statement.setInt(2, data.getType().getValue());
				statement.setBoolean(3, data.hasQuestion());
			});

		int position = connectToNewData(data, lastRowId());
		return position;
	}

	// Does not increment data's count.
	private int connectToNewData(StatementData data,
	                                int           dataId) throws Exception {
		ArrayList<String> columns = new ArrayList<>();
		columns.add("data_id");
		columns.add("text");

		insertTo(statementTables.text, columns,
			statement -> {
				statement.setInt(1, dataId);
				statement.setString(2, data.getText());
			});

		int position = connectToText(data, lastRowId());
		return position;
	}

	// Returns the "rowid" value of the inserted tree row.
	private int connectToData(StatementData data,
	                          int           dataId) throws Exception {
		int position = connectToNewData(data, dataId);
		incrementSingleCount(new Location(statementTables.data, dataId));
		return position;
	}

	// Returns the "rowid" value of the inserted tree row.
	private int connectToText(StatementData data,
	                          int           textId) throws Exception {
		ArrayList<String> columns = new ArrayList<>();
		columns.add("list_id");
		columns.add("text_id");

		insertTo(statementTables.tree, columns,
			statement -> {
				statement.setInt(1, getNextListId());
				statement.setInt(2, textId);
			});

		//database.commit();

		return lastRowId();
	}


	private String parenthesize(ArrayList<String> items) {
		String returnStr = "(";

		for (int i = 0; i < items.size(); ++i) {
			returnStr += items.get(i);
			if (i != items.size() - 1)
				returnStr += ",";
		}

		returnStr += ")";
		return returnStr;
	}

	private int lastRowId() throws SQLException {
		ResultSet generatedKeys = database.createStatement().getGeneratedKeys();
		generatedKeys.next();

		//out.println("Key: " + generatedKeys.getInt(1));

		return generatedKeys.getInt(1);
	}

	private int getNextListId() throws Exception {
		if (position == null) {
			return 1;
		}

		else {
			final int positionId = position;
			ResultSet results = pullRows(statementTables.tree,
				"WHERE rowid=?",
				statement -> {
					statement.setInt(1, positionId);
				});

			assert(results != null);

			results.next();
			int nextListId = results.getInt("rowid");

			if (nextListId != 0) {
				return nextListId;
			}
			else {
				PreparedStatement statement = database.prepareStatement(
					"SELECT max(list_id) FROM " + statementTables.tree + ";"
				);
				ResultSet result = statement.executeQuery();
				result.next();
				return result.getInt(1) + 1;
			}
		}
	}

	private void setPosition(int value) {
		position = value;
	}


	// Increments going down the hierarchy.
	private void incrementCount(Location location) throws Exception {
		if (location.getTableName().equals(statementTables.tree)) {
			// Increment tree.
			incrementSingleCount(location);
			location = nextLayerFromTree(location.rowId);
			location = nextLayerFromText(location.rowId);
			// Increment data.
			incrementSingleCount(location);
		}

		else if (location.getTableName().equals(statementTables.text)) {
			location = nextLayerFromText(location.rowId);
			// Increment data.
			incrementSingleCount(location);
		}

		else if (location.getTableName().equals(statementTables.data)) {
			// Increment data.
			incrementSingleCount(location);
		}
	}

	// Does not go down the hierarchy, only increments the specific location's count.
	private void incrementSingleCount(Location location) throws Exception {
		String tableName = location.getTableName();

		// These are the only tables with "count" columns.
		assert (tableName.equals(statementTables.tree) ||
			      tableName.equals(statementTables.data));

		generalChange(tableName, "UPDATE", "SET count = count+1 WHERE rowid = ?",
			statement -> {
				statement.setInt(1, location.getRowId());
			});
	}


	private Location layerToNextLayer(String tableFrom,
	                                  String tableTo,
	                                  int originalRowId) throws Exception {

		ResultSet set = pullRow(new Location(tableFrom, originalRowId));

		assert (set != null);
		set.next();

		return new Location(tableTo, set.getInt("rowid"));
	}


	private Location nextLayerFromTree(int rowId) throws Exception {
		return layerToNextLayer(statementTables.tree, statementTables.text, rowId);
	}

	private Location nextLayerFromText(int rowId) throws Exception {
		return layerToNextLayer(statementTables.text, statementTables.data, rowId);
	}

	private Location nextLayerFromData(int rowId) throws Exception {
		return layerToNextLayer(statementTables.data, statementTables.textSimplified, rowId);
	}
}
