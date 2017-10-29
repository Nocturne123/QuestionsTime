package fr.nocturne123.questionstime;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import fr.nocturne123.questionstime.question.Question;

public class EventHandler {

	@Listener
	public void onReceiveMessage(MessageChannelEvent e) {
		if(e.getSource() != null && e.getSource() instanceof Player) {
			if(QuestionsTime.getInstance().getCurrentQuestion().isPresent()) {
				QuestionsTime instance = QuestionsTime.getInstance();
				Question q = instance.getCurrentQuestion().get();
				if(e.getMessage().toPlain().endsWith("qt>"+q.getAnswer())) {
					QuestionsTime.getInstance().setPlayedQuestion(Optional.empty());
					Player winner = (Player) e.getSource();
					Prize prize = q.getPrize();
					Task.builder().execute(wait -> {
						Sponge.getServer().getOnlinePlayers().forEach(player -> {
							if(player.getUniqueId().equals(winner.getUniqueId()))
									player.sendMessage(Text.join(instance.qtPrefix, 
											Text.builder(" You win !").color(TextColors.YELLOW).style(TextStyles.BOLD).build()));
							else
								player.sendMessage(Text.join(instance.qtPrefix, 
										Text.builder(winner.getName()+" win !").color(TextColors.YELLOW).style(TextStyles.BOLD).build()));
							if(!prize.getItems()[0].getType().equals(ItemTypes.AIR) || (prize.getMoney() > 0 && instance.getEconomy().isPresent()))
							Task.builder().execute(task -> {
								System.out.println("WOWOWO");
								winner.sendMessage(Text.join(instance.qtPrefix, 
										Text.builder(" Here's your rewards :").color(TextColors.YELLOW).style(TextStyles.BOLD).build()));
								if(!prize.getItems()[0].getType().equals(ItemTypes.AIR)) {
									for(int i = 0; i < prize.getItems().length; i++) {
										winner.sendMessage(Text.join(instance.qtPrefix, 
												Text.builder(" • "+prize.getItems()[i].getQuantity()+" * ")
												.color(TextColors.BLUE).build(), instance.readableItemID(prize.getItems()[i])));
										winner.getInventory().offer(prize.getItems()[i].copy());
									}
								}
								if(prize.getMoney() > 0 && instance.getEconomy().isPresent()) {
									EconomyService ecoSevice = instance.getEconomy().get();
									winner.sendMessage(Text.join(instance.qtPrefix, 
											Text.builder(" •"+prize.getMoney()+" ").color(TextColors.BLUE).build(),
											ecoSevice.getDefaultCurrency().getDisplayName()));
									Optional<UniqueAccount> account = ecoSevice.getOrCreateAccount(winner.getUniqueId());
									if(account.isPresent())
										account.get().deposit(ecoSevice.getDefaultCurrency(), BigDecimal.valueOf(prize.getMoney()),
												Cause.of(e.getContext(), instance));
									else
										instance.getLogger().error("The economy account for "+winner.getName()+" can't be found / created.");
								} else if(!instance.getEconomy().isPresent())
									instance.getLogger().info("No Economy Service found.");
							}).async()
							.delayTicks(60)
							.name("[QT]SendWinnerPrize")
							.submit(instance.getContainer().getInstance().get());
						});
						instance.sayNewQuestion();
					}).async().delay(500, TimeUnit.MILLISECONDS)
					.submit(instance.getContainer().getInstance().get());
				} else if(e.getMessage().toPlain().contains("qt>") && e.getSource() instanceof Player) {
					Player p = (Player) e.getSource();
					String answer = e.getMessage().toPlain().substring(e.getMessage().toPlain().lastIndexOf("qt>") + 3);
					p.sendMessage(Text.join(instance.qtPrefix, Text.builder(" "+answer).color(TextColors.YELLOW).style(TextStyles.BOLD).build(),
							Text.builder(" isn't the right answer :(").color(TextColors.RED).style(TextStyles.NONE).build()));
					if(ConfigHandler.isPersonnalAnswer())
						e.setMessageCancelled(true);
					if(q.getMalus().getMoney() > 0 && instance.getEconomy().isPresent()) {
						EconomyService ecoSevice = instance.getEconomy().get();
						p.sendMessage(Text.join(instance.qtPrefix, Text.builder(" You lose ").color(TextColors.RED)
								.append(Text.builder(q.getMalus().getMoney()+" ").color(TextColors.DARK_RED).build()).build(),
								ecoSevice.getDefaultCurrency().getDisplayName()));
						Optional<UniqueAccount> account = ecoSevice.getOrCreateAccount(p.getUniqueId());
						if(account.isPresent())
							account.get().withdraw(ecoSevice.getDefaultCurrency(), BigDecimal.valueOf(q.getMalus().getMoney()), 
									Cause.of(e.getContext(), instance));
						else
							instance.getLogger().error("The economy account for "+p.getName()+" can't be found / created.");
					} else if(!instance.getEconomy().isPresent())
						instance.getLogger().info("No Economy Service found.");
				}
			}
		}
	}
	
	@Listener
	public void onPlayerDisconnected(ClientConnectionEvent.Disconnect e) {
		if(Sponge.getGame().getServer().getOnlinePlayers().size() == 1 && QuestionsTime.getInstance().getCurrentQuestion().isPresent()) {
			QuestionsTime.getInstance().setPlayedQuestion(Optional.empty());
			QuestionsTime.getInstance().getLogger().info("The last player connected has been disconnected while a question was said. The question has been stopped.");
			QuestionsTime.getInstance().sayNewQuestion();
		}
	}
	
	
	
}
