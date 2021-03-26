package edu.kit.ipd.pronat.topic_extraction_common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jan Keim
 * @author Sebastian Weigelt
 *
 */
public class SimpleTopicExtractionTest {
	private static final Logger logger = LoggerFactory.getLogger(SimpleTopicExtractionTest.class);
	static TopicExtractionCore topicExtraction;

	private static final boolean fileLog = true;
	private static String logFile = "logfile.txt";

	@BeforeClass
	public static void beforeClass() {
		topicExtraction = new TopicExtractionCore();

		// reset log-file
		if (fileLog) {
			final File file = new File(logFile);
			if (file.exists()) {
				file.delete();

			}
			try {
				file.createNewFile();
			} catch (final IOException e) {
				logger.error(e.getMessage(), e.getCause());
			}
		}

	}

	private void test(Collection<String> wordSenses, String name) {
		final List<Topic> resultingList = topicExtraction.getTopicsForSenses(wordSenses);
		if (fileLog) {
			final StringBuilder strBuilder = new StringBuilder(name + "\n");
			strBuilder.append(String.join(",", wordSenses));
			strBuilder.append("\n\n");
			for (final Topic t : resultingList) {
				strBuilder.append(t.getLabel());
				strBuilder.append(" (" + String.join(", ", t.getRelatedSenses()) + ")");
				strBuilder.append("\n");
			}
			strBuilder.append("---------------------------------\n");
			final String out = strBuilder.toString();
			writeToFile(logFile, out);
			logger.debug(out);
		}
	}

	private void writeToFile(String path, String text) {
		// get file, create it, if not yet created
		final File file = new File(path);
		try {
			file.createNewFile();
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		// write to file
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
			writer.append(text);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDroneOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("gate");
		wordSenses.add("degree (angle)");
		wordSenses.add("obstacle");
		wordSenses.add("trail");
		wordSenses.add("gate");
		wordSenses.add("line (geometry)");
		wordSenses.add("earth's surface"); // for ground
		test(wordSenses, "drone1.1");
	}

	@Ignore
	@Test
	public void testDroneOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("gate");
		wordSenses.add("gate");
		wordSenses.add("obstacle");
		wordSenses.add("gate");
		wordSenses.add("line (geometry)");
		wordSenses.add("line (geometry)");
		wordSenses.add("earth's surface"); // for ground
		test(wordSenses, "drone1.2");
	}

	@Ignore
	@Test
	public void testGardenOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("lawn");
		wordSenses.add("mower");
		wordSenses.add("grass");
		test(wordSenses, "Garden1.1");
	}

	@Ignore
	@Test
	public void testGardenOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("shed");
		wordSenses.add("mower");
		wordSenses.add("lawn");
		test(wordSenses, "Garden1.2");
	}

	@Ignore
	@Test
	public void testGardenOneThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("tree");
		wordSenses.add("hedge");
		test(wordSenses, "Garden1.3");
	}

	@Ignore
	@Test
	public void testGardenOneFour() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("saw");
		wordSenses.add("table (furniture)");
		wordSenses.add("table (furniture)");
		test(wordSenses, "Garden1.4");
	}

	@Ignore
	@Test
	public void testGardenOneFive() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("tank");
		wordSenses.add("rake");
		wordSenses.add("shed");
		test(wordSenses, "Garden1.5");
	}

	@Ignore
	@Test
	public void testBarOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("tonic water");
		wordSenses.add("glass (drinkware)");
		wordSenses.add("gin");
		test(wordSenses, "Bar1.1");
	}

	@Ignore
	@Test
	public void testBarOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("cuba");
		wordSenses.add("cola");
		wordSenses.add("rum");
		wordSenses.add("lime (fruit)");
		wordSenses.add("juice");
		wordSenses.add("glass (drinkware)");
		test(wordSenses, "Bar1.2");
	}

	@Ignore
	@Test
	public void testBarOneThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("champagne");
		wordSenses.add("champagne");
		wordSenses.add("glass");
		wordSenses.add("counter (furniture)");
		test(wordSenses, "Bar1.3");
	}

	@Ignore
	@Test
	public void testBarOneFour() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("lime (fruit)");
		wordSenses.add("juice");
		wordSenses.add("cocktail");
		wordSenses.add("cocktail shaker");
		wordSenses.add("basil");
		wordSenses.add("syrup");
		wordSenses.add("gin");
		wordSenses.add("cocktail");
		wordSenses.add("glass (drinkware)");
		wordSenses.add("counter (furniture)");
		test(wordSenses, "Bar1.4");
	}

	@Ignore
	@Test
	public void testBarOneFive() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("beer");
		wordSenses.add("table (furniture)");
		test(wordSenses, "Bar1.5");
	}

	@Ignore
	@Test
	public void testOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("popcorn");
		wordSenses.add("popcorn");
		wordSenses.add("hand");
		test(wordSenses, "1.1");
	}

	@Ignore
	@Test
	public void testOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		test(wordSenses, "1.2");
	}

	@Ignore
	@Test
	public void testOneThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("fridge");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("refrigerator");
		wordSenses.add("juice");
		test(wordSenses, "1.3");
	}

	@Ignore
	@Test
	public void testTwoOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("popcorn");
		wordSenses.add("bag");
		test(wordSenses, "2.1");
	}

	@Ignore
	@Test
	public void testTwoTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		test(wordSenses, "2.2");
	}

	@Ignore
	@Test
	public void testTwoThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("refrigerator");
		test(wordSenses, "2.3");
	}

	@Ignore
	@Test
	public void testThreeOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("popcorn");
		wordSenses.add("kitchen");
		wordSenses.add("table (furniture)");
		test(wordSenses, "3.1");
	}

	@Ignore
	@Test
	public void testThreeTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("cup");
		wordSenses.add("kitchen");
		wordSenses.add("table (furniture)");
		wordSenses.add("dishwasher");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		test(wordSenses, "3.2");
	}

	@Ignore
	@Test
	public void testThreeThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		test(wordSenses, "3.3");
	}

	@Ignore
	@Test
	public void testFourOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("popcorn");
		wordSenses.add("bag");
		test(wordSenses, "4.1");
	}

	@Ignore
	@Test
	public void testFourTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		test(wordSenses, "4.2");
	}

	@Ignore
	@Test
	public void testFourThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		test(wordSenses, "4.3");
	}

	@Ignore
	@Test
	public void testIfFourOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("cupboard");
		test(wordSenses, "If.4.1");
	}

	@Ignore
	@Test
	public void testIfFourTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("cupboard");
		test(wordSenses, "If.4.2");
	}

	@Ignore
	@Test
	public void testIfFourThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("dishware");
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		wordSenses.add("cupboard");
		test(wordSenses, "If.4.3");
	}

	@Ignore
	@Test
	public void testIfFiveOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("vodka");
		test(wordSenses, "If.5.1");
	}

	@Ignore
	@Test
	public void testIfFiveTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		test(wordSenses, "If.5.2");
	}

	@Ignore
	@Test
	public void testIfFiveThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("orange (fruit)");
		wordSenses.add("refrigerator");
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		test(wordSenses, "If.5.3");
	}

	@Ignore
	@Test
	public void testSSevenPEight() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("plate (dishware)");
		wordSenses.add("dishwasher");
		wordSenses.add("water");
		wordSenses.add("sink");
		wordSenses.add("refrigerator");
		wordSenses.add("refrigerator");
		wordSenses.add("food");
		wordSenses.add("plate (dishware)");
		wordSenses.add("refrigerator");
		wordSenses.add("plate (dishware)");
		wordSenses.add("microwave");
		wordSenses.add("door");
		wordSenses.add("table (furniture)");
		test(wordSenses, "s7p08");
	}

	@Ignore
	@Test
	public void testSSevenPNine() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("plate (dishware)");
		wordSenses.add("dishwasher");
		wordSenses.add("meal");
		wordSenses.add("refrigerator");
		wordSenses.add("plate (dishware)");
		wordSenses.add("microwave");
		wordSenses.add("table (furniture)");
		test(wordSenses, "s7p09");
	}

	@Ignore
	@Test
	public void testSSevenPTen() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("dishwasher");
		wordSenses.add("plate (dishware)");
		wordSenses.add("plate (dishware)");
		wordSenses.add("refrigerator");
		wordSenses.add("meal");
		wordSenses.add("meal");
		wordSenses.add("plate (dishware)");
		wordSenses.add("microwave");
		wordSenses.add("microwave");
		wordSenses.add("meal");
		wordSenses.add("plate (dishware)");
		wordSenses.add("plate (dishware)");
		wordSenses.add("table (furniture)");
		test(wordSenses, "s7p10");
	}

	@Ignore
	@Test
	public void testSEightPOne() {
		final String id = "s8p01";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPTwo() {
		final String id = "s8p02";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("push-button");
		wordSenses.add("machine");
		wordSenses.add("door");
		wordSenses.add("laundry");
		wordSenses.add("machine");
		wordSenses.add("clothes dryer");
		wordSenses.add("push-button");
		wordSenses.add("clothes dryer");
		wordSenses.add("door");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("door");
		wordSenses.add("push-button");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPThree() {
		final String id = "s8p03";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		wordSenses.add("program (machine)");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPFour() {
		final String id = "s8p04";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("basement");
		wordSenses.add("laundry");
		wordSenses.add("room");
		wordSenses.add("laundry");
		wordSenses.add("machine");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes");
		wordSenses.add("door");
		wordSenses.add("program (machine)");
		wordSenses.add("minute");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPFive() {
		final String id = "s8p05";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("machine");
		wordSenses.add("action (philosophy)"); // for process
		wordSenses.add("action (philosophy)"); // for process
		wordSenses.add("clothes dryer");
		wordSenses.add("door");
		wordSenses.add("door");
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("door");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPSix() {
		final String id = "s8p06";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		wordSenses.add("machine");
		wordSenses.add("machine");
		wordSenses.add("window");
		wordSenses.add("machine");
		wordSenses.add("arm");
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("laundry");
		wordSenses.add("machine");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		wordSenses.add("push-button");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPSeven() {
		final String id = "s8p07";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("clothes");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPEight() {
		final String id = "s8p08";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("machine");
		wordSenses.add("clothes dryer");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPNine() {
		final String id = "s8p09";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("laundry");
		wordSenses.add("basket");
		wordSenses.add("clothes dryer");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("program (machine)");
		test(wordSenses, id);
	}

	@Ignore
	@Test
	public void testSEightPTen() {
		final String id = "s8p10";
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("clothes dryer");
		wordSenses.add("door");
		wordSenses.add("machine");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		test(wordSenses, id);
	}
}
