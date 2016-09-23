package bridgerrholt.chatbot;

import bridgerrholt.sqlite_interface.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.*;

import java.sql.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Random;


public class ChatBot
{
	private Connection  database = null;
	private InputStream in       = System.in;
	private PrintStream out      = System.out;

	private static ArrayList<String> tableNames;

	static {
		tableNames = new ArrayList<>();
		tableNames.add("statement_tree");
		tableNames.add("statement_text");
		tableNames.add("statement_text_simplified");
	}

	public ChatBot() {

	}

	public ChatBot(InputStream in, PrintStream out) {
		this.in  = in;
		this.out = out;
	}


	public static void createAndRun(String databaseFile) throws Exception {
		ChatBot chatBot = new ChatBot();
		chatBot.run(databaseFile);
	}


	public void run(String databaseFile) throws Exception {
		try {
			database = Database.createConnection(databaseFile);

			setupTables();
		}

		finally {
			if (database != null) database.close();
		}

		/*Statement statement = null;
		PreparedStatement preparedStatement = null;
		Random random = new Random(System.currentTimeMillis());

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

	private void setupTables() throws Exception {
		assert(database != null);

		try (Statement statement = database.createStatement()) {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new FileReader("resources/table_layouts.json"));
			JsonElement json = gson.fromJson(reader, JsonElement.class);
			JsonObject root = json.getAsJsonObject();
			JsonArray current;
			String toExecute = null;

			for (String i : tableNames) {
				current = root.get(i).getAsJsonArray();

				ResultSet result = statement.executeQuery(
					"SELECT name FROM sqlite_master WHERE type='table' AND name='" + i + "';"
				);

				if (!result.next()) {
					toExecute = "CREATE TABLE " + i + "(";
					for (int j = 0; j < current.size(); ++j) {
						toExecute += current.get(j).getAsString();
						if (j != current.size() - 1)
							toExecute += ",";
					}
					toExecute += ");";
				}

				assert (toExecute != null && !toExecute.isEmpty());

				System.out.println(toExecute);

				statement.execute(toExecute);
			}
		}
	}

	private void addRow(ResultSet resultSet, String textDisplay, int type) throws SQLException {
		resultSet.moveToInsertRow();
		resultSet.updateInt("list_id", 0);
		resultSet.updateInt("next_list_id", 0);
		resultSet.updateInt("type", type);
		resultSet.updateString("text_display", textDisplay);
		resultSet.updateString("text_simple", simplifyText(textDisplay));
		resultSet.insertRow();
	}

	private String simplifyText(String text) {
		return text.replaceAll("[^a-zA-Z ]", "").toLowerCase();
	}
}
