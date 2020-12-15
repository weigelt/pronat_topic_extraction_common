package edu.kit.ipd.parse.topic_extraction_common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicExtractionEval {
	private static final Logger logger = LoggerFactory.getLogger(TopicExtractionEval.class);
	static TopicExtractionCore topicExtraction;

	private static final boolean fileLog = true;
	private static URL logFile = TopicExtractionEval.class.getResource("logfile.txt");

	@BeforeClass
	public static void beforeClass() {
		topicExtraction = new TopicExtractionCore();

		// reset log-file
		if (fileLog) {
			try {
				if (logFile == null) {
					final String resPath = TopicExtractionEval.class.getResource(".").getPath();
					final File file = new File(resPath + File.separator + "logfile.txt");
					file.createNewFile();
				} else {
					final File file = new File(logFile.toURI());
					if (file.exists()) {
						file.delete();
						file.createNewFile();
					}
				}
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
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
			this.writeToFile(logFile, out);
			logger.debug(out);
		}
	}

	private void writeToFile(URL path, String text) {
		// get file, create it, if not yet created
		final File file = new File(path.getFile());
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

	// 1
	@Test
	public void testOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("popcorn");
		wordSenses.add("popcorn");
		wordSenses.add("hand");
		this.test(wordSenses, "1.1");
	}

	@Test
	public void testTwoOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("popcorn");
		wordSenses.add("bag");
		this.test(wordSenses, "2.1");
	}

	// 2
	@Test
	public void testThirtyOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("cup");
		wordSenses.add("kitchen");
		wordSenses.add("table (furniture)");
		wordSenses.add("dishwasher");
		this.test(wordSenses, "31.2");
	}

	@Test
	public void testEighteenTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		this.test(wordSenses, "18.2");
	}

	// 3
	@Test
	public void testThreeThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		this.test(wordSenses, "3.3");
	}

	@Test
	public void testTwentyfiveThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		this.test(wordSenses, "25.3");
	}

	// 4
	@Test
	public void testIfFourOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("cupboard");
		this.test(wordSenses, "If.4.1");
	}

	@Test
	public void testIfFourTen() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("dishware");
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		wordSenses.add("dishware");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		wordSenses.add("table (furniture)");
		wordSenses.add("dishware");
		wordSenses.add("dishware");
		wordSenses.add("cupboard");
		wordSenses.add("cupboard");
		wordSenses.add("dishware");
		wordSenses.add("cupboard");
		this.test(wordSenses, "If.4.10");
	}

	// 5
	@Test
	public void testIfFiveFive() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("drink");
		wordSenses.add("refrigerator");
		wordSenses.add("orange (fruit)");
		wordSenses.add("orange (fruit)");
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		wordSenses.add("vodka");
		this.test(wordSenses, "If.5.5");
	}

	@Test
	public void testIfFiveTwelve() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("fridge");
		wordSenses.add("orange (fruit)");
		wordSenses.add("vodka");
		wordSenses.add("orange (fruit)");
		wordSenses.add("juice");
		this.test(wordSenses, "If.5.12");
	}

	// 6
	@Test
	public void testSSixPThree() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("table (furniture)");
		wordSenses.add("cup");
		wordSenses.add("table (furniture)");
		wordSenses.add("refrigerator");
		wordSenses.add("refrigerator");
		wordSenses.add("water");
		wordSenses.add("bottle");
		wordSenses.add("bottle");
		wordSenses.add("water");
		wordSenses.add("cup");
		wordSenses.add("bottle");
		wordSenses.add("fridge");
		wordSenses.add("fridge");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		wordSenses.add("dishwasher");
		wordSenses.add("cup");
		wordSenses.add("cupboard");
		wordSenses.add("cupboard");
		wordSenses.add("cup");
		wordSenses.add("shelf (storage)");
		wordSenses.add("cupboard");
		this.test(wordSenses, "s6p03");
	}

	@Test
	public void testSSixPTen() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("fridge");
		wordSenses.add("refrigerator");
		wordSenses.add("door");
		wordSenses.add("water");
		wordSenses.add("bottle");
		wordSenses.add("refrigerator");
		wordSenses.add("door");
		wordSenses.add("table (furniture)");
		wordSenses.add("water");
		wordSenses.add("bottle");
		wordSenses.add("cup");
		wordSenses.add("water");
		wordSenses.add("cup");
		wordSenses.add("dishwasher");
		wordSenses.add("cupboard");
		this.test(wordSenses, "s6p10");
	}

	// 7
	@Test
	public void testSSevenPEight() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("plate (dishware)");
		wordSenses.add("dishwasher");
		wordSenses.add("water");
		wordSenses.add("sink");
		wordSenses.add("fridge");
		wordSenses.add("fridge");
		wordSenses.add("food");
		wordSenses.add("plate (dishware)");
		wordSenses.add("fridge");
		wordSenses.add("plate (dishware)");
		wordSenses.add("microwave");
		wordSenses.add("door");
		wordSenses.add("table (furniture)");
		this.test(wordSenses, "s7p08");
	}

	@Test
	public void testSSevenPTen() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("dishwasher");
		wordSenses.add("plate (dishware)");
		wordSenses.add("plate (dishware)");
		wordSenses.add("fridge");
		wordSenses.add("meal");
		wordSenses.add("plate (dishware)");
		wordSenses.add("microwave");
		wordSenses.add("microwave");
		wordSenses.add("meal");
		wordSenses.add("plate (dishware)");
		wordSenses.add("plate (dishware)");
		wordSenses.add("table (furniture)");
		this.test(wordSenses, "s7p10");
	}

	// 8
	@Test
	public void testSEightPOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("machine");
		wordSenses.add("laundry");
		wordSenses.add("laundry");
		wordSenses.add("clothes dryer");
		wordSenses.add("clothes dryer");
		this.test(wordSenses, "s8p01");
	}

	@Test
	public void testSEightPSix() {
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
		this.test(wordSenses, "s8p06");
	}

	// 9
	@Test
	public void testDroneOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("gate");
		wordSenses.add("degree (angle)");
		wordSenses.add("table (furniture)");
		wordSenses.add("greenhouse");
		wordSenses.add("pond");
		wordSenses.add("pond");
		wordSenses.add("lawn");
		this.test(wordSenses, "drone1.1");
	}

	@Test
	public void testDroneOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("gate");
		wordSenses.add("greenhouse");
		wordSenses.add("table (furniture)");
		wordSenses.add("greenhouse");
		wordSenses.add("pond");
		wordSenses.add("lawn");
		this.test(wordSenses, "drone1.2");
	}

	// 10
	@Test
	public void testMindstormOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("line (geometry)");
		wordSenses.add("carpet");
		wordSenses.add("carpet");
		wordSenses.add("rattle (percussion instrument)");
		wordSenses.add("rattle (percussion instrument)");
		this.test(wordSenses, "mindstorm1.1");
	}

	@Test
	public void testMindstormOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("line (geometry)");
		wordSenses.add("carpet");
		wordSenses.add("rattle (percussion instrument)");
		this.test(wordSenses, "mindstorm1.2");
	}

	// 11
	@Test
	public void testAlexaOneOne() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("temperature");
		wordSenses.add("radiator");
		wordSenses.add("degree (temperature)");
		wordSenses.add("playlist");
		this.test(wordSenses, "alexa1.1");
	}

	@Test
	public void testAlexaOneTwo() {
		final List<String> wordSenses = new ArrayList<>();
		wordSenses.add("playlist");
		wordSenses.add("radiator");
		this.test(wordSenses, "alexa1.2");
	}

}
