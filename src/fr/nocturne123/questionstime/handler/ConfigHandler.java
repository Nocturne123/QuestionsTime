package fr.nocturne123.questionstime.handler;

import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.util.TextUtils;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.command.source.ConsoleSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigHandler {

	private static Path configFile;
	private static ConfigurationLoader<CommentedConfigurationNode> configLoader;
	private static CommentedConfigurationNode configNode;
	private static final QuestionsTime main = QuestionsTime.getInstance();
	private static final ConsoleSource console = QuestionsTime.getInstance().getConsole();

	private static int cooldown;
	private static boolean isRandom;
	private static int minCooldown;
	private static int maxCooldown;
	private static boolean personalAnswer;
	private static int minConnected;
	
	public static void init(Path path) {
		try {
			Preconditions.checkNotNull(path, "The file instance which is essential to access the config file was null. Try to restart the server ?");
			console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("Loading config..."));
			if(Files.notExists(Paths.get(path.toString(), "config.conf"))) {
				main.getContainer().getAsset("config.conf")
						.ifPresent(asset -> {
							try {
								asset.copyToDirectory(path, false, true);
								console.sendMessage(
										TextUtils.Console.creatorNormalWithPrefix("The config file was not found, the default config file has been created"));
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
			}
			
			configFile = path;
			configLoader = HoconConfigurationLoader.builder().setFile(new File(path.toString(), "config.conf")).build();
			configNode = configLoader.load();
			cooldown = configNode.getNode("cooldown").getInt();
			isRandom = configNode.getNode("randomTime").getBoolean();
			minCooldown = configNode.getNode("minCooldown").getInt();
			maxCooldown = configNode.getNode("maxCooldown").getInt();
			personalAnswer = configNode.getNode("personalAnswer").getBoolean();
			minConnected = configNode.getNode("minConnected").getInt();
			if(minConnected <= 0)
				minConnected = 1;
			if(cooldown <= 0)
				cooldown = 200;
			if(minCooldown <= 0)
				minCooldown = 200;
			if(maxCooldown <= 0)
				maxCooldown = 400;
			
			CommentedConfigurationNode questions = configNode.getNode("questions");
			loadQuestions(questions);
			configLoader.save(configNode);
		} catch (Exception e) {
			QuestionsTime.getInstance().getConsole().sendMessage(TextUtils.Console.creatorError("Error during loading the config file !"));
			e.printStackTrace();
		}
	}
	
	private static void loadQuestions(CommentedConfigurationNode nodeBase) {
		nodeBase.getChildrenMap().forEach((key, questionNodeInfo) -> {
			try {
				Question question = questionNodeInfo.getValue(TypeToken.of(Question.class));
				if (question != null) {
					main.addQuestion(question);
					console.sendMessage(TextUtils.Console.creatorComposed(" The question ", "" + key, " is loaded"));
				} else
					console.sendMessage(TextUtils.Console.creatorComposed("Error when reading the question",
							"" + key, ""));
			} catch (ObjectMappingException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void serializeQuestion(Question question) {
		try {
			ConfigurationNode node = configNode.getNode("questions", question.getQuestion().replace('?', ' ').replaceAll(" ", ""));
			node.setValue(TypeToken.of(Question.class), question);
			configLoader.save(configNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isRandomTick() {
		return isRandom;
	}
	
	public static int getCooldown() {
		return cooldown;
	}
	
	public static int getMinCooldown() {
		return minCooldown;
	}
	
	public static int getMaxCooldown() {
		return maxCooldown;
	}
	
	public static boolean isPersonalAnswer() {
		return personalAnswer;
	}

	public static int getMinConnected() {
		return minConnected;
	}

}
