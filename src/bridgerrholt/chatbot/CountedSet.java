package bridgerrholt.chatbot;

import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

class CountedSet
{
	private ResultSet resultSet;

	public class GradedSet
	{
		private int count;
		private int bestId = 0;

		public GradedSet(ResultSet set) throws Exception {
			Random random = new Random(System.currentTimeMillis());
			ArrayList<Integer> ids = new ArrayList<>();

			while (set.next()) {
				int id    = set.getInt("rowid");
				int count = set.getInt("count");
				for (int i = 0; i < count; ++i) {
					ids.add(id);
				}
			}

			bestId = ids.get(random.nextInt(ids.size()));

			/*Random random = new Random(System.currentTimeMillis());
			ArrayList<AtomicInteger> maxValues = new ArrayList<>();
			ArrayList<Integer>       ids       = new ArrayList<>();

			while (set.next()) {
				maxValues.add(new AtomicInteger(set.getInt("count") + 1));
				ids.add(set.getInt("rowid"));
			}

			count = maxValues.size();

			if (count == 0) return;

			int bestIndex = 0;

			while (true) {
				ArrayList<Integer> highestIndexes = new ArrayList<>();
				Integer highestGrade = 0;

				for (int i = 0; i < maxValues.size(); ++i) {
					Integer grade = random.nextInt(maxValues.get(i).get());

					if (grade > highestGrade) {
						highestGrade = grade;
						highestIndexes.clear();
					}

					if (grade >= highestGrade) {
						highestIndexes.add(i);
					}
				}

				if (highestIndexes.size() == 1) {
					bestIndex = highestIndexes.get(0);
					break;
				}
				else {
					int idToHelp = random.nextInt(highestIndexes.size());
					maxValues.get(highestIndexes.get(idToHelp)).incrementAndGet();
				}
			}

			bestId = ids.get(bestIndex);*/
		}

		public boolean hasValues() { return (count > 0); }
		public int     getBestId() { return bestId; }
	}

	public CountedSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public GradedSet grade() throws Exception {
		return new GradedSet(resultSet);
	}



}
