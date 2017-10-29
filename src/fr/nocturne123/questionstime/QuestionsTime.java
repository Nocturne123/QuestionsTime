package fr.nocturne123.questionstime;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.google.inject.Inject;

import fr.nocturne123.questionstime.question.Question;

@Plugin(id = "questionstime", name = "QuestionsTime", version = "1.0.2", description = "Ask questions and gain prize for the winner", authors = {"Nocturne123" })
public class QuestionsTime {

	private static QuestionsTime instance;

	@Inject
	private Logger logger;

	@Inject
	private Game game;
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private File file;
	
	@Inject
	private PluginContainer container;
	
	private ArrayList<Question> questions;
	public final Text qtPrefix = Text.builder("[").color(TextColors.AQUA)
			.append(Text.builder("QT").color(TextColors.YELLOW)
			.append(Text.builder("]").color(TextColors.AQUA)
			.build()).build()).build();
	private Optional<Question> currentQuestion;
	private Optional<EconomyService> economy;

	@SuppressWarnings("static-access")
	@Listener
	public void onServerInitialization(GameInitializationEvent e) {
		this.questions = new ArrayList<>();
		this.instance = this;
		if(file != null)
			ConfigHandler.init(file);
		else
			this.logger.warn("The file instance which is essential to access the config file was null. This shouldn't never happen. Try to restart the server ?");
		this.currentQuestion = Optional.empty();
		this.economy = Sponge.getServiceManager().provide(EconomyService.class);
		Sponge.getEventManager().registerListeners(this, new EventHandler());	
		//TODO: Appliquer damage sur item prize
	}

	@Listener
	public void onServerStart(GameStartedServerEvent e) {
		if(this.questions.size() > 0) {
			this.logger.info("Loaded "+this.questions.size()+" questions !");
			this.sayNewQuestion();
		} else
			this.logger.error("No questions were register. Do you add questions ?");
	}

	@Listener
	public void onServerStop(GameStoppedEvent e) {
		
	}
	
	@Listener
	public void onServerProviderChange(ChangeServiceProviderEvent e) {
		if(e.getService().equals(EconomyService.class))
			this.economy = Optional.of((EconomyService) e.getNewProviderRegistration().getProvider());
	}
	
	@Listener
	public void onPlayerConnected(ClientConnectionEvent.Login event) {
//		User p = event.getTargetUser();
//		p.getInventory().offer(ItemStack.builder().itemType(ItemTypes.STONE).quantity(1).add(Keys.ITEM_DURABILITY, 1).build());
//		for(Question q : this.questions) {
//			for(int i = 0; i < q.getPrize().getItems().length; i++) {
//				p.getInventory().offer(q.getPrize().getItems()[i].copy());
//			}
//		}
	}
	
	public void sayNewQuestion() {
		if(this.questions.size() > 0) {
			if(ConfigHandler.isRandomTick())
				this.startIntervalQuestion();
			else
				this.startFixTimeQuestion();
		}
	}
	
	private void startFixTimeQuestion() {
		Task.builder().execute(new QuestionTask(false, this))
		.async()
		.delayTicks(ConfigHandler.getCooldown())
		.name("[QT]FixTimeQuestion")
		.submit(this);
	}
	
	private void startIntervalQuestion() {
		Task.builder().execute(task -> {
			Task.builder().execute(new QuestionTask(true, this))
			.async()
			.delayTicks(RandomUtils.nextInt(1, ConfigHandler.getMaxCooldown() - ConfigHandler.getMinCooldown()))
			.name("[QT]SecondIntervalQuestion")
			.submit(this);
		}).async()
		  .delayTicks(ConfigHandler.getMinCooldown())
		  .name("[QT]MainIntervalQuestion")
		  .submit(this);
	}
	
	public Text readableItemID(ItemStack is) {
		String itemID = is.getType().getId();
		if(itemID.isEmpty())
			return Text.of("itemIDEmpty");
		String finalID = "";
		Text textModID = Text.builder("").build();
		if(itemID.startsWith("minecraft:")) {
			finalID = itemID.substring(10);
			finalID = finalID.substring(0, 1).toUpperCase()+finalID.substring(1, finalID.length());
		} else if(itemID.contains(":") && !itemID.split(":")[0].isEmpty()) {
			String modID = itemID.split(":")[0];
			Optional<PluginContainer> pluginCont = Sponge.getPluginManager().getPlugin(modID);
			if(pluginCont.isPresent()) {
				String modName = pluginCont.get().getName();
				String itemName = itemID.split(":")[1];
				textModID = Text.builder(modName.substring(0, 1).toUpperCase()+modName.substring(1, modName.length())+": ").color(TextColors.BLUE).build();
				finalID = itemName.substring(0, 1).toUpperCase()+itemName.substring(1, itemName.length());
			} else {
				this.logger.error("No mod with ID \""+modID+"\" was found.");
				finalID = modID+" - "+itemID+" -> No found";
			}
		} else
			finalID = "error{"+itemID+"}";
		if(finalID.contains("_"))
			finalID = finalID.replace('_', ' ');
		Map<DataQuery, Object> keys = is.toContainer().getValues(true);
		Text metadataText = Text.builder().build();
		if(keys.containsKey(DataQuery.of("UnsafeDamage")) && keys.get(DataQuery.of("UnsafeDamage")) instanceof Integer)
			if(((int) keys.get(DataQuery.of("UnsafeDamage"))) != 0)
				metadataText = Text.builder(" "+keys.get(DataQuery.of("UnsafeDamage"))).color(TextColors.AQUA).build();
		return Text.join(textModID, Text.builder(finalID).color(TextColors.WHITE).build(), metadataText);
	}

//	@Listener
//	public void onPlayerJoin(ClientConnectionEvent.Join e) {
//		Player p = e.getTargetEntity();
//
//		p.sendMessage(Text.of("Bienvenue ", TextColors.GOLD, p.getName(), TextColors.BLUE, " !"));
//	}

	public Game getGame() {
		return game;
	}

	public Logger getLogger() {
		return logger;
	}

	public static QuestionsTime getInstance() {
		return instance;
	}
	
	public void addQuestion(Question question) {
		this.questions.add(question);
	}
	
	public ArrayList<Question> getQuestions() {
		return this.questions;
	}
	
	public Optional<Question> getCurrentQuestion() {
		return this.currentQuestion;
	}
	
	public void setPlayedQuestion(Optional<Question> currentQuestion) {
		this.currentQuestion = currentQuestion;
	}
	
	public PluginContainer getContainer() {
		return this.container;
	}
	
	public Optional<EconomyService> getEconomy() {
		return economy;
	}

}
