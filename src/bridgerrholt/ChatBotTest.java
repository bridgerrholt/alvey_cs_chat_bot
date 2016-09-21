package bridgerrholt;

import bridgerrholt.chat_bot.ChatBot;

public class ChatBotTest {
	public static void main(String[] args) {
		try {
			ChatBot bot = new ChatBot();
		}

		catch (Exception e) {
			System.out.println("Main function caught: " + e.getMessage());
		}
	}
}
