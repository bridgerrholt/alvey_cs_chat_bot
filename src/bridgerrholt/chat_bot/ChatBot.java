package bridgerrholt.chat_bot;

import bridgerrholt.sqlite_interface.*;

public class ChatBot
{
	private Database database;

	public ChatBot() throws Exception {
		database = new Database("primary_database.db");
	}

	public ChatBot(String databaseFile)
}
