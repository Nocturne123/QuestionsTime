package fr.nocturne123.questionstime;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.google.inject.Inject;

@Plugin(id = "questionstime", name = "QuestionsTime", version = "1.0.0", description = "Ask questions and gain prize for the winner", authors = {"Nocturne123" })
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
		//CommandSpec command = CommandSpec.builder().description(Text.of("Permet de savoir ses coordonn�es"))
		//		.executor(new Command())
				// .arguments(GenericArguments.remainingJoinedStrings(Text.of("get")))
		//		.build();
		
		//TODO: Appliquer damage sur item prize
		//TODO: annonce prize bug
		
	//	game.getCommandManager().register(this, command, "coord");
		// game.getEventManager().registerListener(this, new GadjetThunder(), this);
//		game.getEventManager().registerListeners(this, new GadgetThunder());
//		game.getEventManager().registerListeners(this, new Inventory());
		// Sponge.getRegistry().register(WorldGeneratorModifier.class, new
		// WorldManipulation());
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
		
//		for(Question q : this.questions) {
//			for(int i = 0; i < q.getPrize().getItems().length; i++)
//				System.out.println(q.getPrize().getItems()[i]);
//		}
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
		
//		for(Question q : this.questions) {
//			for(int i = 0; i < q.getPrize().getItems().length; i++)
//				System.out.println(q.getPrize().getItems()[i]);
//		}
	}

//	@Listener
//	public void onPlayerJoin(ClientConnectionEvent.Join e) {
//		Player p = e.getTargetEntity();
//
//		p.sendMessage(Text.of("Bienvenue ", TextColors.GOLD, p.getName(), TextColors.BLUE, " !"));
//	}

	// @Listener
	// public void onInteractBlock(InteractBlockEvent.Secondary e){
	// Optional<Player> p = e.getCause().first(Player.class);
	// BlockSnapshot block = e.getTargetBlock();
	// logger.debug("test1");
	// if(block == BlockTypes.DIRT){
	// logger.debug("test2");
	// if(p.isPresent()){
	// logger.debug("test3");
	// Player pl = p.get();
	// pl.sendMessage(Text.builder("Tu viens de casser un block de dirt
	// !").color(TextColors.AQUA).build());
	// }
	// }
	// }
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
