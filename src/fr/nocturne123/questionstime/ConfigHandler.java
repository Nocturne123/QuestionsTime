package fr.nocturne123.questionstime;

import java.io.File;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.asset.Asset;

import com.google.common.base.Preconditions;

import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionMulti;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

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
			
			if(!file.exists()) {
				Asset jarConfigFile = QuestionsTime.getInstance().getGame().getPluginManager().getPlugin("questionstime").get().getAsset("defaultconfig.conf").get();
				jarConfigFile.copyToFile(file.toPath(), false, true);
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
			//configNode.getNode("is").setValue(TypeToken.of(ItemStack.class), ItemStack.builder().itemType(ItemTypes.STONE).quantity(5).build());
			configLoader.save(configNode);
		} catch (Exception e) {
			QuestionsTime.getInstance().getLogger().error("Error during loading the config file !");
			e.printStackTrace();
		}
	}
	
	private static void loadQuestions(CommentedConfigurationNode nodeBase) {
		nodeBase.getChildrenMap().entrySet().forEach(questionNode -> {
			CommentedConfigurationNode questionNodeInfo = questionNode.getValue();
			Types questionType = Question.getType(questionNodeInfo);
			
			if(questionType == Types.ERROR) {
				QuestionsTime.getInstance().getLogger().warn("The question "+questionNode.getKey()+" contain one or many errors. "
						+ "Check if he contain the sections \"question\" and \"answer\" at least. ");
				return;
			}
			
			String question = questionNodeInfo.getNode("question").getString();
			String answer = questionNodeInfo.getNode("answer").getString();
			CommentedConfigurationNode prize = questionNodeInfo.getNode("prize");
			CommentedConfigurationNode malus = questionNodeInfo.getNode("malus");
			
			if(questionType == Types.SIMPLE) {
				Question questionSimple = new Question(question, prize == null ? Optional.empty() : Optional.of(prize), answer, Optional.of(malus));
				QuestionsTime.getInstance().addQuestion(questionSimple);
			} else if(questionType == Types.MULTI) {
				if(StringUtils.isNumeric(answer) && Integer.valueOf(answer) <= 4 && Integer.valueOf(answer) > 0) {
					byte answerNumber = Byte.valueOf(answer);
					String propositionOne = questionNodeInfo.getNode("proposition1").getString();
					CommentedConfigurationNode propositionTwo = questionNodeInfo.getNode("proposition2");
					CommentedConfigurationNode propositionThree = questionNodeInfo.getNode("proposition3");
					CommentedConfigurationNode propositionFour = questionNodeInfo.getNode("proposition4");
					QuestionMulti questionMulti = new QuestionMulti(question, prize == null ? Optional.empty() : Optional.of(prize),
							new String[] {propositionOne, propositionTwo == null ? "" : propositionTwo.getString(),
									 propositionThree == null ? "" : propositionThree.getString(),
									 propositionFour == null ? "" : propositionFour.getString()}, answerNumber, Optional.of(malus));
					QuestionsTime.getInstance().addQuestion(questionMulti);
				}
			} else
				QuestionsTime.getInstance().getLogger().error("Houston, This shouldn't never happen !");
			QuestionsTime.getInstance().getLogger().info("The question \""+questionNode.getKey()+"\" is loaded !");
		});
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
