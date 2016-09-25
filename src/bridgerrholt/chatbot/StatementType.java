package bridgerrholt.chatbot;

class StatementType
{
	static final int
		BASIC = 0,
		ANSWER = 1,
		GREETING = 2;


	StatementType(int value) throws Exception {
		if (value < MIN || value > MAX)
			throw new Exception("Invalid value for StatementData.Type");

		this.value = value;
	}

	int getValue() { return value; }


	private static final int
		MIN = BASIC,
		MAX = GREETING;

	private int value;
}