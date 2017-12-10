package fr.nocturne123.questionstime;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionMulti;

public class QuestionTask implements Runnable {

	private boolean isInterval;
	private QuestionsTime instance;
	
	public QuestionTask(boolean isInterval, QuestionsTime instance) {
		this.isInterval = isInterval;
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
					player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" It's Question Time !").color(TextColors.YELLOW).build()));
			});
			
			Task.builder().execute(task -> {
				instance.getGame().getServer().getOnlinePlayers().forEach(player -> {
					if(instance.isCreator(player.getUniqueId()))
						return;
					player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" "+q.getQuestion()).color(TextColors.YELLOW).style(TextStyles.BOLD).build()));
	
					if(q.getType() == Types.MULTI) {
						QuestionMulti qMulti = (QuestionMulti) q;
						for(int i = 0; i < qMulti.getPropositions().length; i++)
							player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" •"+(i+1)+"] "+qMulti.getPropositions()[i]).color(TextColors.AQUA).build()));
					}
					if(prize.isAnnounce() && 
							((prize.getItems().length > 0 && !prize.getItems()[0].getItem().equals(ItemTypes.NONE)) || 
									(prize.getMoney() >= 0 && instance.getEconomy().isPresent()))) {
						player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" The winner win : ").color(TextColors.YELLOW).build()));
						if(prize.getMoney() > 0 && QuestionsTime.getInstance().getEconomy().isPresent())
							player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" •"+prize.getMoney()+" ").color(TextColors.BLUE).build(),
									instance.getEconomy().get().getDefaultCurrency().getDisplayName()));
						for(int i = 0; i < prize.getItems().length; i++) {
							ItemStack is = prize.getItems()[i];
							if(!is.getItem().equals(ItemTypes.NONE)) {
								player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" • "+is.getQuantity()+" * ").color(TextColors.BLUE).build(), 
										TextUtils.readableItemID(is)));
							}
						}
					}
					if(malus.isAnnounce() && malus.getMoney() > 0 && instance.getEconomy().isPresent()) {
						player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" But a wrong answer : ").color(TextColors.RED).build()));
						player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" • -"+malus.getMoney()+" ").color(TextColors.DARK_RED).build(),
								instance.getEconomy().get().getDefaultCurrency().getDisplayName()));
					}
					player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" Answer with : \"").color(TextColors.YELLOW).append(
							Text.builder("qt>answer").color(TextColors.AQUA).append(Text.builder("\"").color(TextColors.YELLOW).build()).build()).build()));
					player.sendMessage(Text.join(instance.qtPrefix, Text.builder(" May the best win !").color(TextColors.YELLOW).build()));
					instance.setPlayedQuestion(Optional.of(q));
				});
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



}
