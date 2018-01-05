package fr.nocturne123.questionstime.handler;

import java.io.File;

import org.spongepowered.api.asset.Asset;

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

public class ConfigHandler {

	private static File configFile;
	private static ConfigurationLoader<CommentedConfigurationNode> configLoader;
	private static CommentedConfigurationNode configNode;
	
	private static int cooldown;
	private static boolean isRandom;
	private static int minCooldown;
	private static int maxCooldown;
	private static boolean personnalAnswer;
	private static int minConnected;
	
	public static void init(File file) {
		try {
			Preconditions.checkNotNull(file, "The file instance which is essential to access the config file was null. Try to restart the server ?");
			QuestionsTime.getInstance().getConsole().sendMessage(TextUtils.Console.creatorNormalWithPrefix("Loading config..."));
			
			if(!file.exists()) {
				Asset jarConfigFile = QuestionsTime.getInstance().getContainer().getAsset("config.conf").get();
				jarConfigFile.copyToFile(file.toPath());
				QuestionsTime.getInstance().getConsole().sendMessage(
						TextUtils.Console.creatorNormalWithPrefix("The config file was not found, the default config file has been created"));
			}
			
			configFile = file;
			configLoader = HoconConfigurationLoader.builder().setFile(configFile).build();
			configNode = configLoader.load();
			cooldown = configNode.getNode("cooldown").getInt();
			isRandom = configNode.getNode("randomTime").getBoolean();
			minCooldown = configNode.getNode("minCooldown").getInt();
			maxCooldown = configNode.getNode("maxCooldown").getInt();
			personnalAnswer = configNode.getNode("personnalAnswer").getBoolean();
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
		nodeBase.getChildrenMap().entrySet().forEach(questionNode -> {
			CommentedConfigurationNode questionNodeInfo = questionNode.getValue();
			try {
				Question question = questionNodeInfo.getValue(TypeToken.of(Question.class));
				if(question != null) {
					QuestionsTime.getInstance().addQuestion(question);
					QuestionsTime.getInstance().getConsole().sendMessage(TextUtils.Console.creatorComposed(" The question ", ""+questionNode.getKey(), " is loaded"));
				} else
					QuestionsTime.getInstance().getConsole().sendMessage(TextUtils.Console.creatorComposed("Error when reading the question",
							""+questionNode.getKey(), ""));
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
	
	public static boolean isPersonnalAnswer() {
		return personnalAnswer;
	}
	
	public static int getMinConnected() {
		return minConnected;
	}

}
