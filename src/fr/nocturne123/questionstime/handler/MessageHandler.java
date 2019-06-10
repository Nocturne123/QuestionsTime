package fr.nocturne123.questionstime.handler;

import com.google.common.collect.Lists;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.util.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class MessageHandler {

	private static HashMap<String, String> messages;
	private static ConsoleSource console = QuestionsTime.getInstance().getConsole();
	
	public static void init(Path path) {
		try {
			if(path != null) {
				console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("Loading messages..."));
				if(Files.notExists(Paths.get(path.toString(), "message.conf"))) {
					QuestionsTime.getInstance().getContainer().getAsset("message.conf")
							.ifPresent(asset -> {
								try {
									asset.copyToDirectory(path, false, true);
									console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("The message file was not found, the default message file has been created"));
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
					loadDefaultMessages();
				} else {
					loadDefaultMessages();
					loadMessages(Paths.get(path.toString(), "message.conf"));
				}
			} else {
				console.sendMessage(
						TextUtils.Console.creatorNormalWithPrefix("The message.conf was not found, the default messages will be used"));
				loadDefaultMessages();
			}
		} catch(Exception e) {
			console.sendMessage(TextUtils.Console.creatorError("Error when loading the message.conf"));
			e.printStackTrace();
		}
	}
	
	private static void loadMessages(Path path) {
		try {
			int succesfullMessages = 0;
			ArrayList<String> lines = new ArrayList<>(Files.readAllLines(path));
			for(String line : lines) {
				if(line.isEmpty() || line.startsWith("#"))
					continue;
				String section = getSection(line);
				if(messages.containsKey(section)) {
					String text = getText(line);
					if(!text.isEmpty()) {
						ArrayList<String> componentsDefault = getComponents(messages.get(section));
						ArrayList<String> componentsText = getComponents(text);
						Collections.sort(componentsDefault);
						Collections.sort(componentsText);
						if(componentsDefault.equals(componentsText)) {
							if(!text.equals(messages.get(section))) {
								messages.put(section, text);
								succesfullMessages++;
							}
						} else {
							if(componentsDefault.get(0).isEmpty())
								console.sendMessage(TextUtils.Console.creatorError("  The section \""+section+"\" shall not have components"));
							else if(componentsText.size() > componentsDefault.size())
								console.sendMessage(TextUtils.Console.creatorError("  The section \""+section+"\" have more component(s) that needed, he only need : "+componentsDefault));
							else if(componentsText.size() == componentsDefault.size())
								console.sendMessage(TextUtils.Console.creatorError("  The section \""+section+"\" doesn't have the right component(s), he need : "+componentsDefault));
							else if(componentsText.size() < componentsDefault.size())
								console.sendMessage(TextUtils.Console.creatorError("  The section \""+section+"\" have less component(s) that needed, he need : "+componentsDefault));
						}
					} else 
						console.sendMessage(TextUtils.Console.creatorError("  Error when reading the text for the section \""+section+"\""));
				} else
					console.sendMessage(TextUtils.Console.creatorError("  Unknow section \""+section+"\""));
			};
			console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("Messages loaded, "+succesfullMessages+"/"+messages.size()+" replaced"));
		} catch (Exception e) {
			console.sendMessage(TextUtils.Console.creatorError("Error when reading \"messages.conf\""));
			e.printStackTrace();
		}
	}
	
	private static String getSection(String line) {
		if(!line.isEmpty()) {
			for(int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if(c == ':')
					return i > 0 ? line.substring(0, i) : "";
			}
		}
		return "";
	}
	
	private static String getText(String line) {
		if(!line.isEmpty()) {
			for(int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				if(c == ':')
					return i > 0  && line.length()-1 > i ? line.substring(i+1, line.length()) : "";
			}
		}
		return "";
	}
	
	
	
	private static ArrayList<String> getComponents(String text) {
		if(!text.isEmpty()) {
			String[] components = StringUtils.substringsBetween(text, "{", "}");
			ArrayList<String> listComponents = Lists.newArrayList(components != null ? components : new String[] {""});
			return listComponents;
		}
		return Lists.newArrayList();
	}
	
	private static void loadDefaultMessages() {
		messages = new HashMap<>();
		messages.put("question.new", " §eIt's Question Time !");
		messages.put("question.ask", " §e§l{question}");
		messages.put("question.proposition", " §b•{position}] {proposition}");
		messages.put("prize.announce", " §eThe winner win :");
		messages.put("prize.money", " §9•{money} §r{currency}");
		messages.put("prize.item", " §9• {quantity} * {modid}§f{item} §b{metadata}"); //{customname} {lore}
		messages.put("malus.announce", " §cBut a wrong answer :");
		messages.put("malus.money", " §4• -{money} §r{currency}");
		messages.put("answer.announce", " §eAnswer with : \"§bqt>answer§e\"");
		messages.put("question.end", " §eMay the best win !");
		messages.put("question.end.timer", " §eYou have §9§l{timer}§r§e to answer ! May the best win !");
		messages.put("answer.win", " §e§lYou win !");
		messages.put("answer.win.announce", " §e§l{name} win !");
		messages.put("reward.prize.announce", " §e§lHere's your reward :");
		messages.put("reward.prize", " §9• {quantity} * {modid}§f{item} §b{metadata}");
		messages.put("reward.money", " §9•{money} §r{currency}");
		messages.put("answer.false", " §e§l{answer} §cisn't the right answer :(");
		messages.put("answer.malus", " §cYou lose §4{money} §r{currency}");
		messages.put("question.timer.left", " §eYou have §9§l{timer}§r§e to answer !");
		messages.put("question.timer.out", " §cNobody have found the answer, maybe a next time");
	}
	
	public static Text get(String section) {
		if(messages.containsKey(section)) {
			String message = messages.get(section);
			if(getComponents(message).get(0).isEmpty())
				return Text.of(message);
			else
				return Text.EMPTY;
		}
		return Text.EMPTY;
	}
	
	public static Text get(String section, String... components) {
		if(messages.containsKey(section)) {
			String message = messages.get(section);
			String customName = "";
			ArrayList<Text> lore = Lists.newArrayList();
			ArrayList<String> componentList = getComponents(message);
			if(!componentList.get(0).isEmpty() && components.length == componentList.size()) {
				for(int i = 0; i < components.length; i++) {
					String component = components[i];
					if(component.startsWith("money") && componentList.contains("money"))
						message = message.replace("{money}", component.substring(6));
					else if(component.startsWith("question") && componentList.contains("question"))
						message = message.replace("{question}", component.substring(9));
					else if(component.startsWith("position") && componentList.contains("position"))
						message = message.replace("{position}", component.substring(9));
					else if(component.startsWith("proposition") && componentList.contains("proposition"))
						message = message.replace("{proposition}", component.substring(12));
					else if(component.startsWith("currency") && componentList.contains("currency"))
						message = message.replace("{currency}", component.substring(9));
					else if(component.startsWith("quantity") && componentList.contains("quantity"))
						message = message.replace("{quantity}", component.substring(9));
					else if(component.startsWith("modid") && componentList.contains("modid"))
						message = message.replace("{modid}", correctModID(component));
					else if(component.startsWith("item") && componentList.contains("item"))
						message = message.replace("{item}", correctItem(component));
					else if(component.startsWith("metadata") && componentList.contains("metadata"))
						message = message.replace("{metadata}", correctMetadata(component));
					else if(component.startsWith("name") && componentList.contains("name"))
						message = message.replace("{name}", component.substring(5));
					else if(component.startsWith("answer") && componentList.contains("answer"))
						message = message.replace("{answer}", component.substring(7));
					else if(component.startsWith("timer") && componentList.contains("timer"))
						message = message.replace("{timer}", readeableTimer(component));
					/*else if(component.startsWith("customname") && componentList.contains("customname"))
						customName = correctCustomName(message, component);*/
				}
				return Text.builder(message)/*.onHover(TextActions.showText(Text.of(customName)))*/.build();
			} else
				return Text.EMPTY;
		}
		return Text.EMPTY;
	}
	
	private static String correctModID(String component) {
		if(!component.isEmpty() && component.length() > 6) {
			String modID = component.substring(6);
			if(modID.startsWith("minecraft"))
				return "";
			else {
				Optional<PluginContainer> pluginCont = Sponge.getPluginManager().getPlugin(modID);
				if(pluginCont.isPresent()) {
					modID = modID.substring(0, 1).toUpperCase()+modID.substring(1, modID.length())+": ";
					return modID;
				} else
					return "error[cmID]:no-"+modID+"-found";
			}
		} else
			return "error[cmID]:empty-OR-under-or-equal-6";
	}
	
	private static String correctItem(String component) {
		if(!component.isEmpty() && component.length() > 5) {
			String itemID = component.substring(5);
			itemID = itemID.substring(0, 1).toUpperCase()+itemID.substring(1, itemID.length());
			return itemID;
		} else
			return "error[ci]:empty-OR-under-or-equal-5";
	}
	
	private static String correctMetadata(String component) {
		if(!component.isEmpty() && component.length() > 9) {
			String metadata = component.substring(9);
			if(StringUtils.isNumeric(metadata) && Integer.valueOf(metadata) > 0)
				return metadata;
			else
				return "";
		} else
			return "error[cm]:empty-OR-under-or-equal-9";
	}
	
	private static String readeableTimer(String component) {
		if(!component.isEmpty() && component.length() > 6) {
			String timer = component.substring(6);
			if(StringUtils.isNumeric(timer)) {
				int second = Integer.valueOf(timer);
				if(second < 60)
					return second+" sec";
				else if(second < 3600)
					return (second / 60)+"min"+(second % 60)+"sec";
				else
					return (second / 3600)+"h"+((second % 3600) / 60)+"min"+((second % 3600) % 60)+"sec";
			} else
				return "error[rt]:no-numeric";
		} else
			return "error[rt]:empty-OR-under-or-equal-6";
	}
	
	private static String correctCustomName(String message, String customNameComponent) {
		
		return "";
	}
	
}
