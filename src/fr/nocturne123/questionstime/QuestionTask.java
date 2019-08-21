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
import org.slf4j.Logger;
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
			Logger logger = instance.getLogger();
			Random rand = new Random();
			ArrayList<Question> questions = instance.getQuestions();
			Question question = null;

			final int totalWeight = questions.stream().mapToInt(Question::getWeight).sum();
			logger.debug("totalWeight: "+totalWeight);
			int weight = rand.nextInt(totalWeight);
			logger.debug("random weight: "+weight);
			for(Question questionList : questions) {
				logger.debug("question weight: "+questionList.getWeight());
				weight -= questionList.getWeight();
				logger.debug("new weight value: "+weight);
				if(weight < 0) {
					question = questionList;
					logger.debug("question chosen: '"+questionList.getQuestion()+"'");
					break;
				}
			}
			final Question finalQuestion = question;
			if(finalQuestion == null) {
				logger.warn("No questions chosen. It's not normal, please report with debug.log or latest.log");
				return;
			}

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
							.setComponent(MessageComponents.QUESTION, finalQuestion.getQuestion()).build())));
					
					if(finalQuestion.getType() == Types.MULTI) {
						QuestionMulti qMulti = (QuestionMulti) finalQuestion;
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
					if(finalQuestion.isTimed())
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.QUESTION_TIMER_END)
								.setComponent(MessageComponents.TIMER, finalQuestion.getTimer()).build())));
					else
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.QUESTION_END)));
				});
				if(finalQuestion.isTimed()) this.startTimer(finalQuestion.getTimer());
				instance.setPlayedQuestion(finalQuestion);
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
		System.out.println("START TIMER "+timerTicks);
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
