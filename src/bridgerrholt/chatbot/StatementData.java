package bridgerrholt.chatbot;

class StatementData
{
	/*enum Type
	{
		BASIC(0), ANSWER(1), GREETING(2);

		private static final int min = BASIC.getValue(),
			                       max = GREETING.getValue();
		private final int value;

		private Type(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static Type createFromInt(int value) throws Exception {
			assert (value >= min && value <= max);

			switch (value) {
				case 0:
					return BASIC;
				case 1:
					return ANSWER;
				case 2:
					return GREETING;

				default:
					throw new Exception("Invalid StatementData.Type value");
			}
		}
	}*/

	String  getText()           { return text; }
	String  getTextSimplified() { return textSimplified; }
	int     getType()           { return type.getValue(); }
	boolean hasQuestion()       { return hasQuestion; }


	StatementData(String text, StatementType type) {
		this.text = removeExtraSpaces(text);
		this.type = type;

		hasQuestion = text.contains("?");

		simplifyText();
	}



	private String        text;
	private String        textSimplified;
	private StatementType type;
	private boolean       hasQuestion;

	// Removes from and end spaces as well as double spaces.
	private String removeExtraSpaces(String text) {
		// The simplified string to be returned.
		String returnText = "";

		// Whether the previous character checked was a space character.
		boolean previousSpace = false;

		for (int i = 0, length = text.length(); i < length; ++i) {
			Character c = text.charAt(i);

			if (Character.isSpaceChar(c)) {
				if (!previousSpace) {
					previousSpace = true;
					returnText += c;
				}
			}

			else {
				previousSpace = false;
				returnText += c;
			}
		}

		return returnText.trim();
	}

	// Lower-cases the text, keeping only characters and single spaces.
	private void simplifyText() {
		textSimplified = "";

		for (int i = 0, length = text.length(); i < length; ++i) {
			Character c = text.charAt(i);

			if (Character.isAlphabetic(c) || Character.isDigit(c) || Character.isSpaceChar(c)) {
				textSimplified += Character.toLowerCase(c);
			}
		}
	}
}
