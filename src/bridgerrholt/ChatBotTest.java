package bridgerrholt;

import bridgerrholt.chatbot.ChatBot;

public class ChatBotTest {
	public static void main(String[] args) throws Exception {
		try {
			ChatBot.createAndRun("databases/primary.db");
		}

		catch (Exception e) {
			System.out.println("Main function caught: " + e.getMessage());
			throw e;
		}
	}
}
