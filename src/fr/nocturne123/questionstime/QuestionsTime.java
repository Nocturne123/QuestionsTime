package fr.nocturne123.questionstime;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import fr.nocturne123.questionstime.handler.ConfigHandler;
import fr.nocturne123.questionstime.handler.EventHandler;
import fr.nocturne123.questionstime.handler.MessageHandler;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.QuestionCreator;
import fr.nocturne123.questionstime.question.QuestionSerializer;
import fr.nocturne123.questionstime.util.TextUtils;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

@Plugin(id = "questionstime", name = "QuestionsTime", version = "1.1.2", description = "Ask questions and gain prize for the winner", authors = {"Nocturne123"})
public class QuestionsTime {

	private static QuestionsTime instance;

	@Inject
	private Logger logger;

	@Inject
	private Game game;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path path;
	
	@Inject
	private PluginContainer container;
	
	private ArrayList<Question> questions;
	public final Text qtPrefix = Text.builder("[").color(TextColors.AQUA)
			.append(Text.builder("QT").color(TextColors.YELLOW)
			.append(Text.builder("]").color(TextColors.AQUA)
			.build()).build()).build();
	private Optional<Question> currentQuestion;
	private Optional<EconomyService> economy;
	private ArrayList<QuestionCreator> questionCreator;
	private long timedQuestionStarted;
	private Task taskTimer;

	@SuppressWarnings("static-access")
	@Listener
	public void onServerInitialization(GameInitializationEvent e) {
		this.instance = this;
		this.getConsole().sendMessage(TextUtils.Console.creatorNormalWithPrefix("~~~QUESTIONSTIME~~~"));
		TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(Question.class), new QuestionSerializer());
		this.questions = new ArrayList<>();
		if(path != null) {
			ConfigHandler.init(path/*new File(path.toString() + "/questionstime.conf")*/);
			MessageHandler.init(path/*new File(path.toString() + "/message.conf")*/);
		} else
			this.logger.warn("The file instance which is essential to access the config file was null. This shouldn't never happen. Try to restart the server ?");
		this.currentQuestion = Optional.empty();
		this.questionCreator = new ArrayList<>();
		this.economy = Sponge.getServiceManager().provide(EconomyService.class);
		Sponge.getEventManager().registerListeners(this, new EventHandler());
	
		CommandSpec commandQTCreate = CommandSpec.builder()
				.description(Text.builder("Create a question easily").color(TextColors.GREEN).build())
				.permission("questionstime.command.create")
				.executor(new CommandCreateQuestion())
				.build();
				
		CommandSpec commandQTBase = CommandSpec.builder()
				.description(Text.builder("The main QuestionsTime command").color(TextColors.YELLOW).build())
				.permission("questionstime.command.base")
				.child(commandQTCreate, "create")
				.build();
		Sponge.getCommandManager().register(this, commandQTBase, "questionstime", "qt");
	}

	@Listener
	public void onServerStart(GameStartedServerEvent e) {
		if(this.questions.size() > 0) {
			this.getConsole().sendMessage(TextUtils.Console.creatorComposed("Loaded ", ""+this.questions.size(), " question(s) !"));
			this.sayNewQuestion();
		} else
			this.getConsole().sendMessage(TextUtils.Console.creatorError("No questions were register. Do you add questions ?"));
	}

	@Listener
	public void onServerStop(GameStoppedEvent e) {
		
	}
	
	@Listener
	public void onServerProviderChange(ChangeServiceProviderEvent e) {
		if(e.getService().equals(EconomyService.class))
			this.economy = Optional.of((EconomyService) e.getNewProviderRegistration().getProvider());
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

	public void addCreator(UUID uuid) {
		this.questionCreator.add(new QuestionCreator(uuid));
	}
	
	public void removeCreator(UUID uuid) {
		this.questionCreator.removeIf(qc -> qc.getCreator().equals(uuid));

		/*Iterator<QuestionCreator> iter = this.questionCreator.iterator();
		while(iter.hasNext()) {
			QuestionCreator qc = iter.next();
			if(qc.getCreator().equals(uuid)) {
				iter.remove();
				break;
			}
		}*/
	}
	
	public boolean isCreator(UUID uuid) {
		for(QuestionCreator qc : this.questionCreator) {
			if(qc.getCreator().equals(uuid))
				return true;
		}
		return false;
	}
	
	public Optional<QuestionCreator> getQuestionCreator(UUID uuid) {
		for(QuestionCreator qc : this.questionCreator) {
			if(qc.getCreator().equals(uuid))
				return Optional.of(qc);
		}
		return Optional.empty();
	}

	public int getSpongeAPI() {
		char version = Sponge.getPlatform().getContainer(Component.API).getVersion().orElse("0").charAt(0);
		return StringUtils.isNumeric(String.valueOf(version)) ? CharUtils.toIntValue(version) : 0;
	}
	
	public ConsoleSource getConsole() {
		return Sponge.getGame().getServer().getConsole();
	}
		
	public void startTimer(Task task) {
		this.timedQuestionStarted = System.currentTimeMillis();
		this.taskTimer = task;
	}
	
	public long getTimerStarted() {
		return System.currentTimeMillis() - this.timedQuestionStarted;
	}
	
	public void stopTimer() {
		this.taskTimer.cancel();
		this.timedQuestionStarted = 0;
	}
}
