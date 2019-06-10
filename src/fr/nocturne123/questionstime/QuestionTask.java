package fr.nocturne123.questionstime;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import fr.nocturne123.questionstime.handler.ConfigHandler;
import fr.nocturne123.questionstime.handler.MessageHandler;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionMulti;
import fr.nocturne123.questionstime.util.TextUtils;

public class QuestionTask implements Runnable {

	private QuestionsTime instance;
	
	public QuestionTask(boolean isInterval, QuestionsTime instance) {
		this.instance = instance;
	}
	
	@Override
	public void run() {
		if(instance.getGame().getServer().getOnlinePlayers().size() >= ConfigHandler.getMinConnected()) {
			Random rand = new Random();
			ArrayList<Question> qs = instance.getQuestions();
			Question q = qs.get(rand.nextInt(qs.size()));
			Prize prize = q.getPrize();
			Malus malus = q.getMalus();
			instance.getGame().getServer().getOnlinePlayers().forEach(player -> {
				if(!instance.isCreator(player.getUniqueId()))
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("question.new")));
			});
			
			Task.builder().execute(task -> {
				instance.getGame().getServer().getOnlinePlayers().forEach(player -> {
					if(instance.isCreator(player.getUniqueId()))
						return;
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("question.ask", "question:"+q.getQuestion())));
					
					if(q.getType() == Types.MULTI) {
						QuestionMulti qMulti = (QuestionMulti) q;
						for(int i = 0; i < qMulti.getPropositions().length; i++)
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("question.proposition", "position:"+(i+1), "proposition:"+qMulti.getPropositions()[i])));
					}
					if(prize.isAnnounce() && 
							((prize.getTypes().length > 0 && !prize.getTypes()[0].getType().equals(ItemTypes.NONE)) ||
									(prize.getMoney() >= 0 && instance.getEconomy().isPresent()))) {
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("prize.announce")));
						if(prize.getMoney() > 0 && QuestionsTime.getInstance().getEconomy().isPresent())
							player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("prize.money", "money:"+prize.getMoney(), "currency:"+
									instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain())));
						
						for(int i = 0; i < prize.getTypes().length; i++) {
							ItemStack is = prize.getTypes()[i];
							if(!is.getType().equals(ItemTypes.NONE))
								player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("prize.item", "quantity:"+is.getQuantity(),
										"modid:"+is.getType().getId().split(":")[0], "item:"+is.getType().getId().split(":")[1], "metadata:"+is.toContainer().getValues(true)
										.get(DataQuery.of("UnsafeDamage")), "customname:"+is.get(Keys.DISPLAY_NAME).orElse(Text.of("null")),
										"lore:"+is.get(Keys.ITEM_LORE).orElse(Collections.singletonList(Text.of("null"))))));
						}
					}
					if(malus.isAnnounce() && malus.getMoney() > 0 && instance.getEconomy().isPresent()) {
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("malus.announce")));
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("malus.money", "money:"+malus.getMoney(),
								"currency:"+instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain())));
					}
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("answer.announce")));
					if(q.isTimed()) {
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("question.end.timer", "timer:"+q.getTimer())));
						this.startTimer(q.getTimer());
					} else
						player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("question.end")));
				});
				instance.setPlayedQuestion(Optional.of(q));
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
				TextUtils.sendTextToEveryone(MessageHandler.get("question.timer.out"));
				instance.setPlayedQuestion(Optional.empty());
				instance.stopTimer();
				instance.sayNewQuestion();
			} else if(timeLeft % 3600 == 0 || timeLeft == 1800 || timeLeft == 900 || timeLeft == 300 || timeLeft == 60 || timeLeft == 30 || timeLeft == 15 
					|| timeLeft == 5 || timeLeft == 4 || timeLeft == 3 || timeLeft == 2 || timeLeft == 1)
				TextUtils.sendTextToEveryone(MessageHandler.get("question.timer.left", "timer:"+timeLeft));
		})
		.async()
		.name("[QT]QuestionTimer")
		.interval(1, TimeUnit.SECONDS)
		.submit(instance);
		instance.startTimer(task);
	}


}
