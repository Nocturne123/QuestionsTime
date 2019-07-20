package fr.nocturne123.questionstime;

import fr.nocturne123.questionstime.handler.ConfigHandler;
import fr.nocturne123.questionstime.handler.MessageHandler;
import fr.nocturne123.questionstime.message.Message;
import fr.nocturne123.questionstime.message.MessageComponents;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionMulti;
import fr.nocturne123.questionstime.question.component.Malus;
import fr.nocturne123.questionstime.question.component.Prize;
import fr.nocturne123.questionstime.util.TextUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class QuestionTask implements Runnable {

	private QuestionsTime instance;
	
	public QuestionTask(QuestionsTime instance) {
		this.instance = instance;
	}
	
	@Override
	public void run() {
		if(instance.getGame().getServer().getOnlinePlayers().size() >= ConfigHandler.getMinConnected()) {
			Random rand = new Random();
			ArrayList<Question> questions = instance.getQuestions();
			Question question = questions.get(rand.nextInt(questions.size()));
			Optional<Prize> prizeOptional = question.getPrize();
			Optional<Malus> malusOptional = question.getMalus();
			instance.getGame().getServer().getOnlinePlayers().forEach(player -> {
				if(!instance.isCreator(player.getUniqueId()))
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.QUESTION_NEW)));
			});
			
			Task.builder().execute(task -> {
				instance.getGame().getServer().getOnlinePlayers().forEach(player -> {
					if(instance.isCreator(player.getUniqueId()))
						return;
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.QUESTION_ASK)
							.setComponent(MessageComponents.QUESTION, question.getQuestion()).build())));
					
					if(question.getType() == Types.MULTI) {
						QuestionMulti qMulti = (QuestionMulti) question;
						for(int i = 0; i < qMulti.getPropositions().size(); i++)
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.QUESTION_PROPOSITION)
									.setComponent(MessageComponents.POSITION, (byte) (i+1))
									.setComponent(MessageComponents.PROPOSITION, qMulti.getPropositions().get(i))
									.build())));
					}

					prizeOptional.ifPresent(prize -> {
						if(prize.isAnnounce() &&
								((prize.getItemStacks().length > 0 && !prize.getItemStacks()[0].getType().equals(ItemTypes.NONE)) ||
										(prize.getMoney() >= 0 && instance.getEconomy().isPresent()))) {
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.PRIZE_ANNOUNCE)));
							if(prize.getMoney() > 0 && QuestionsTime.getInstance().getEconomy().isPresent())
								player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.PRIZE_MONEY)
										.setComponent(MessageComponents.MONEY, prize.getMoney())
										.setComponent(MessageComponents.CURRENCY, instance.getEconomy().get()).build())));

							for(int i = 0; i < prize.getItemStacks().length; i++) {
								ItemStack is = prize.getItemStacks()[i];
								if(!is.getType().equals(ItemTypes.NONE))
									player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.PRIZE_ITEM)
											.setComponent(MessageComponents.QUANTITY, is.getQuantity())
											.setComponent(MessageComponents.MOD_ID, is)
											.setComponent(MessageComponents.ITEM, is)
											.setComponent(MessageComponents.METADATA, is)
											.build())));
							}
						}
					});

					malusOptional.ifPresent(malus -> {
						if(malus.isAnnounce() && malus.getMoney() > 0 && instance.getEconomy().isPresent()) {
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.MALUS_ANNOUNCE)));
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.MALUS_MONEY)
									.setComponent(MessageComponents.MONEY, malus.getMoney())
									.setComponent(MessageComponents.CURRENCY, instance.getEconomy().get()
									).build())));
						}
					});

					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.ANSWER_ANNOUNCE)));
					if(question.isTimed())
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.QUESTION_TIMER_END)
								.setComponent(MessageComponents.TIMER, question.getTimer()).build())));
					else
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.QUESTION_END)));
				});
				if(question.isTimed()) this.startTimer(question.getTimer());
				instance.setPlayedQuestion(question);
			})
			.async()
			.delay(3, TimeUnit.SECONDS)
			.name("[QT]AskQuestion")
			.submit(instance);
		} else {
			instance.getLogger().info("No enough players ("+Sponge.getServer().getOnlinePlayers().size()+"/"+ConfigHandler.getMinConnected()+"), the question will be reported.");
			instance.sayNewQuestion();
		}
	}

	private void startTimer(int timerTicks) {
		Task task = Task.builder().execute(consumer -> {
			long secondStarted = instance.getTimerStarted() / 1000;
			int timeLeft = (int) (timerTicks - secondStarted);
			if(timeLeft == 0) {
				TextUtils.sendTextToEveryone(MessageHandler.get(MessageHandler.Messages.QUESTION_TIMER_OUT));
				instance.setPlayedQuestion(null);
				instance.stopTimer();
				instance.sayNewQuestion();
			} else if(timeLeft % 3600 == 0 || timeLeft == 1800 || timeLeft == 900 || timeLeft == 300 || timeLeft == 60 || timeLeft == 30 || timeLeft == 15 
					|| timeLeft == 5 || timeLeft == 4 || timeLeft == 3 || timeLeft == 2 || timeLeft == 1)
				TextUtils.sendTextToEveryone(MessageHandler.get(Message.builder(MessageHandler.Messages.QUESTION_TIMER_LEFT)
						.setComponent(MessageComponents.TIMER, timeLeft).build()));
		})
		.async()
		.name("[QT]QuestionTimer")
		.delay(1, TimeUnit.SECONDS)
		.interval(1, TimeUnit.SECONDS)
		.submit(instance);
		instance.startTimer(task);
	}


}
