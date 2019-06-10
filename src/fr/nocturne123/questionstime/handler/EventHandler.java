package fr.nocturne123.questionstime.handler;

import fr.nocturne123.questionstime.Prize;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionCreator;
import fr.nocturne123.questionstime.util.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class EventHandler {

	@Listener
	public void onReceiveMessage(MessageChannelEvent e) {
		if(e.getCause() != null && e.getCause().first(Player.class).isPresent() && !e.getCause().containsType(PluginContainer.class)) {
			QuestionsTime instance = QuestionsTime.getInstance();
			Player p = e.getCause().first(Player.class).get();
			String message = e.getMessage().toPlain();
			if(message.endsWith("qtc>start") && !instance.isCreator(p.getUniqueId())) {
				instance.addCreator(p.getUniqueId());
				this.handleQuestionCreation(message, p, instance, e);
				return;
			}
			if(message.contains("qt>") && !instance.isCreator(p.getUniqueId()) && instance.getCurrentQuestion().isPresent())
				this.handleQuestionAnswer(message, p, instance, e);
			if(message.contains("qtc>") && instance.isCreator(p.getUniqueId()))
				this.handleQuestionCreation(message, p, instance, e);
		}
	}
	
	private void handleQuestionAnswer(String message, Player p, QuestionsTime instance, MessageChannelEvent e) {
		Question question = instance.getCurrentQuestion().get();
		if(message.endsWith("qt>"+question.getAnswer())) {
			QuestionsTime.getInstance().setPlayedQuestion(Optional.empty());
			if(question.isTimed()) instance.stopTimer();
			Prize prize = question.getPrize();
			Task.builder().execute(wait -> Sponge.getServer().getOnlinePlayers().forEach(player -> {
				if(player.getUniqueId().equals(p.getUniqueId()))
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("answer.win")));
				else
					player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("answer.win.announce", "name:"+p.getName())));
			})).async().delay(500, TimeUnit.MILLISECONDS)
			.submit(instance.getContainer().getInstance().get());
			
			if(!prize.getTypes()[0].getType().equals(ItemTypes.NONE) || (prize.getMoney() > 0 && instance.getEconomy().isPresent()))
				Task.builder().execute(task -> {
					p.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("reward.prize.announce")));
					if(!prize.getTypes()[0].getType().equals(ItemTypes.NONE)) {
						for(int i = 0; i < prize.getTypes().length; i++) {
							ItemStack item = prize.getTypes()[i];
							p.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("reward.prize", "quantity:"+item.getQuantity(),
									"modid:"+item.getType().getId().split(":")[0], "item:"+item.getType().getId().split(":")[1], "metadata:"+item.toContainer().getValues(true)
									.get(DataQuery.of("UnsafeDamage")))));
							p.getInventory().offer(prize.getTypes()[i].copy());
						}
					}
					if(prize.getMoney() > 0 && instance.getEconomy().isPresent()) {
						EconomyService ecoSevice = instance.getEconomy().get();
						p.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("reward.money", "money:"+prize.getMoney(), "currency:"+ecoSevice
								.getDefaultCurrency().getDisplayName().toPlain())));
						Optional<UniqueAccount> account = ecoSevice.getOrCreateAccount(p.getUniqueId());
						if(account.isPresent())
							account.get().deposit(ecoSevice.getDefaultCurrency(), BigDecimal.valueOf(prize.getMoney()),
									e.getCause());
						else
							instance.getLogger().error("The economy account for "+p.getName()+" can't be found / created.");
					} else if(!instance.getEconomy().isPresent())
						instance.getLogger().info("No Economy Service found.");
				}).async()
				.delayTicks(60)
				.name("[QT]SendWinnerPrize")
				.submit(instance.getContainer().getInstance().get());
			instance.sayNewQuestion();
		} else if(message.contains("qt>")) {
			String answer = message.substring(message.lastIndexOf("qt>") + 3);
			p.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("answer.false", "answer:"+answer)));
			if(ConfigHandler.isPersonalAnswer())
				e.setMessageCancelled(true);
			if(question.getMalus().getMoney() > 0 && instance.getEconomy().isPresent()) {
				EconomyService ecoSevice = instance.getEconomy().get();
				p.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get("answer.malus", "money:"+question.getMalus().getMoney(), "currency:"+ecoSevice
						.getDefaultCurrency().getDisplayName().toPlain())));
				Optional<UniqueAccount> account = ecoSevice.getOrCreateAccount(p.getUniqueId());
				if(account.isPresent())
					account.get().withdraw(ecoSevice.getDefaultCurrency(), BigDecimal.valueOf(question.getMalus().getMoney()), e.getCause());
				else
					instance.getLogger().error("The economy account for "+p.getName()+" can't be found / created.");
			} else if(!instance.getEconomy().isPresent())
				instance.getLogger().info("No Economy Service found.");
		}
	}
	
	private void handleQuestionCreation(String message, Player p, QuestionsTime instance, MessageChannelEvent e) {
		if(instance.getQuestionCreator(p.getUniqueId()).isPresent()) {
			QuestionCreator qc = instance.getQuestionCreator(p.getUniqueId()).get();
			String answer = message.substring(message.lastIndexOf("qtc>") + 4);
			if(answer.equals("stop"))
				qc.setStop();
			if(answer.equals("confirm") && qc.isConfirm())
				qc.nextStep();
			else
				qc.setPreviousResponse(answer);
			
			switch(qc.getCurrentStep()) {
			case -1:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to quit the Question Creator ? The question will not be saved !"));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to continue the creation of the command ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer need to be \"", "qtc>yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to stop the Question Creator ?"));
					p.sendMessage(TextUtils.creatorComposed("Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 0:
				if(qc.isHalfConfirm()) {
					p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right question ?"));
					qc.setConfirm();
				} else {
					p.sendMessage(TextUtils.creatorComposed("What's the question ? Answer with \"", "qtc>question", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 1:
				if(qc.isHalfConfirm()) {
					if(answer.equals("simple")) {
						p.sendMessage(TextUtils.creatorComposed("The question's type is \"", "simple", "\". That's right ?"));
						qc.setConfirm();
					} else if(answer.equals("proposition")) {
						p.sendMessage(TextUtils.creatorComposed("The question's type is \"", "proposition", "\". That's right ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The question's type can only be \"", "simple OR proposition", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(Text.join(TextUtils.creatorComposed("What's the question's type ? Answer with \"", "qtc>simple", "\" or \""), 
							Text.builder("qtc>proposition").color(TextColors.BLUE).build(),
							Text.builder("\"").color(TextColors.GREEN).build()));
					qc.setHalfconfirm();
				}
				break;
			case 2:
				if(qc.isHalfConfirm()) {
					p.sendMessage(TextUtils.creatorComposed("The first proposition is \"", answer, "\". It is correct ?"));
					qc.setConfirm();
				} else {
					p.sendMessage(TextUtils.creatorComposed("What's the first proposition ? Answer with \"", "qtc>proposition1", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 3:
				if(qc.isHalfConfirm()) {
					p.sendMessage(TextUtils.creatorComposed("The second proposition is \"", answer, "\". It is correct ?"));
					qc.setConfirm();
				} else {
					p.sendMessage(TextUtils.creatorComposed("What's the second proposition ? Answer with \"", "qtc>proposition2", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 4:
				if(qc.isHalfConfirm()) {
					p.sendMessage(TextUtils.creatorComposed("The third proposition is \"", answer, "\". It is correct ?"));
					qc.setConfirm();
				} else {
					p.sendMessage(TextUtils.creatorComposed("What's the third proposition ? Answer with \"", "qtc>proposition3", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 5:
				if(qc.isHalfConfirm()) {
					p.sendMessage(TextUtils.creatorComposed("The fourth proposition is \"", answer, "\". It is correct ?"));
					qc.setConfirm();
				} else {
					p.sendMessage(TextUtils.creatorComposed("What's the fourth proposition ? Answer with \"", "qtc>proposition4", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 6:
				if(qc.isConfirm() || qc.isHalfConfirm()) {
					if(qc.getQuestionType() == Types.MULTI) {
						if(answer.equals("1") || answer.equals("2") || answer.equals("3") || answer.equals("4")) {
							p.sendMessage(TextUtils.creatorComposed("The right proposition is \"", answer, "\". It is correct ?"));
							qc.setConfirm();
						} else {
							p.sendMessage(TextUtils.creatorComposed("The right proposition can only be \"", "1 OR 2 OR 3 OR 4", "\""));
							qc.setHalfconfirm();
						}
					} else if(qc.getQuestionType() == Types.SIMPLE) {
						p.sendMessage(TextUtils.creatorComposed("The answer is \"", answer, "\". It is correct ?"));
						qc.setConfirm();
					}
					
				} else if(qc.getQuestionType() == Types.MULTI) {
					p.sendMessage(TextUtils.creatorComposed("What's the right proposition ? Answer with \"", "qtc>1 OR 2 OR 3 OR 4", "\""));
					qc.setHalfconfirm();
				} else if(qc.getQuestionType() == Types.SIMPLE) {
					p.sendMessage(TextUtils.creatorComposed("What's the answer of the question ? Answer with \"", "qtc>answer", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 7:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to add prizes ?")));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add prizes ?")));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Do you want to add prizes ? Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 8:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to announce the prize after the question was asked ?")));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to not announce the prize after the question was asked ?")));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Do you want to announce the prize ? Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 9:
				if(qc.isHalfConfirm()) {
					if(StringUtils.isNumeric(answer)) {
						if(answer.length() <= 18) {
							if(Long.valueOf(answer) > 0) {
								p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right amount ?"));
								qc.setConfirm();
							} else {
								p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add money as a prize ?")));
								qc.setConfirm();
							}
						} else {
							p.sendMessage(TextUtils.creatorComposed("\"", answer,"\" is too big !"));
							qc.setHalfconfirm();
						}
					} else {
						p.sendMessage(TextUtils.creatorComposed("\"", answer,"\" isn't a number or a positive number"));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(Text.join(TextUtils.creatorComposed("What is the amount of ",
							instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain(), " ? Answer with \""), TextUtils.creatorSpecial("qtc>amount"),
							TextUtils.creatorNormal("\" (if you doesn't want, just put 0)")));
					qc.setHalfconfirm();
				}
				break;
			case 10:
				if(qc.isConfirm() && !answer.equals("[ModID]:{ItemID};[Variant];[Count]")) {
					ItemStack is = this.getStackBySyntax(answer, p);
					if(is.getType() != ItemTypes.NONE) {
						qc.addItemPrize(is);
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" Added \""), TextUtils.readableItemID(is), 
								Text.builder(" * "+is.getQuantity()).color(TextColors.LIGHT_PURPLE).build(),
								TextUtils.creatorNormal("\"")));
					} else
						p.sendMessage(TextUtils.creatorComposed("Incorrect syntax : \"", answer, "\""));
				} else {
					p.sendMessage(TextUtils.creatorComposed("Add an item as prize with \"", "qtc>[ModID]:{ItemID};[Variant];[Count]", "\""));
					p.sendMessage(TextUtils.creatorComposed("Which \"", "{...}", "\" is obligatory"));
					p.sendMessage(TextUtils.creatorComposed("And \"", "[...]", "\" optionnal"));
					p.sendMessage(TextUtils.creatorComposed("If you don't want to add items, type \"", "qtc> confirm", "\" directly"));
					qc.setConfirm();
				}
				break;
			case 11:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to add a malus ?"));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not add a malus ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Do you want to add a malus for a wrong answer ? Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 12:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to announce the malus ?"));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not announce the malus ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Do you want to announce the malus after the question ? Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 13:
				if(qc.isHalfConfirm()) {
					if(StringUtils.isNumeric(answer)) {
						if(answer.length() <= 18) {
							if(Long.valueOf(answer) > 0) {
								p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right amount ?"));
								qc.setConfirm();
							} else {
								p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add money as a malus ?")));
								qc.setConfirm();
							}
						} else {
							p.sendMessage(TextUtils.creatorComposed("\"", answer,"\" is too big !"));
							qc.setHalfconfirm();
						}
					} else {
						p.sendMessage(TextUtils.creatorComposed("\"", answer,"\" isn't a number or a positive number"));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(Text.join(TextUtils.creatorComposed("What is the amount of ",
							instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain(), " ? Answer with \""), TextUtils.creatorSpecial("qtc>amount"),
							TextUtils.creatorNormal("\" (if you doesn't want, just put 0)")));
					qc.setHalfconfirm();
				}
				break;
			case 14:
				if(qc.isHalfConfirm()) {
					if(answer.equals("yes")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to add a timer ?"));
						qc.setConfirm();
					} else if(answer.equals("no")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not add a timer ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Do you want to add a timer ? Answer with \"", "qtc>yes OR no", "\""));
					qc.setHalfconfirm();
				}
				break;
			case 15:
				if(qc.isHalfConfirm()) {
					if(Pattern.matches("^[0-9]+(h)[0-9]+(m)[0-9]+(s)$", answer)) {
						String timeHour = StringUtils.substringBefore(answer, "h");
						String timeMinute = StringUtils.substringBetween(answer, "h", "m");
						String timeSecond = StringUtils.substringBetween(answer, "m", "s");
						if(StringUtils.isNumeric(timeHour) && StringUtils.isNumeric(timeMinute) && StringUtils.isNumeric(timeSecond)) {
							int hour = Integer.valueOf(timeHour);
							int min = Integer.valueOf(timeMinute);
							int sec = Integer.valueOf(timeSecond);
							if(hour >= 0 && hour <= 24 ) {
								if(min >= 0 && min <= 59) {
									if(sec >= 0 && sec <= 59) {
										if(hour >= 24) {
											p.sendMessage(TextUtils.creatorComposed("", answer, " can't exceed 23h"));
											qc.setHalfconfirm();
										} else if(hour == 0 && min == 0 && sec < 10) {
											p.sendMessage(TextUtils.creatorComposed("", answer, " can't be under 10 secs"));
											qc.setHalfconfirm();
										} else {
											p.sendMessage(TextUtils.creatorComposed("Is the time ", hour+"H"+min+"M"+sec+"S", " enough to answer the question ?"));
											qc.setConfirm();
										}
									} else {
										p.sendMessage(TextUtils.creatorComposed("", timeSecond, " need to be between 0 and 59"));
										qc.setHalfconfirm();
									}
								} else {
									p.sendMessage(TextUtils.creatorComposed("", timeMinute, " need to be between 0 and 59"));
									qc.setHalfconfirm();
								}
							} else {
								p.sendMessage(TextUtils.creatorComposed("", timeHour, " need to be between 0 and 23"));
								qc.setHalfconfirm();
							}
						} else {
							if(!StringUtils.isNumeric(timeHour))
								p.sendMessage(TextUtils.creatorComposed("", timeHour, " isn't an number"));
							if(!StringUtils.isNumeric(timeMinute))
								p.sendMessage(TextUtils.creatorComposed("", timeMinute, " isn't an number"));
							if(!StringUtils.isNumeric(timeSecond))
								p.sendMessage(TextUtils.creatorComposed("", timeSecond, " isn't an number"));
							qc.setHalfconfirm();
						}
					} else {
						p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorSpecial(" "+answer), TextUtils.creatorNormal(" doesn't matche "), 
								TextUtils.creatorSpecial("xhxmxs")));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(Text.join(TextUtils.creatorComposed("How long the players can answer ? Answer with this format \"", "qtc>xhxmxs", "\", where "),
							TextUtils.creatorSpecial("x"), TextUtils.creatorNormal(" is a number")));
					p.sendMessage(Text.join(TextUtils.creatorComposed("Note : the maximum is ", "23h59m59s", " and the minimum is "), TextUtils.creatorSpecial("0h0m10s")));
					qc.setHalfconfirm();
				}
				break;
			case 16:
				if(qc.isHalfConfirm()) {
					if(answer.equals("start")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to start the question ?"));
						qc.setConfirm();
					} else if(answer.equals("save")) {
						p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to just save the question ?"));
						qc.setConfirm();
					} else {
						p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "start OR save", "\""));
						qc.setHalfconfirm();
					}
				} else {
					p.sendMessage(TextUtils.creatorComposed("Good, it's finish ! The question is now registered ! Do you want to start the question or just save it ? "
							+ "Answer with \"", "qtc>start OR save", "\""));
					qc.setHalfconfirm();
				}
				break;
			}
			e.setMessageCancelled(true);
		} else
			instance.getLogger().error("I think a spacetime error occurred because this is -normally- impossible to happen. But, yea, I think you found a bug. It is cool ?");
	}
	
	/**Syntax : [ModID]:{ItemID};[Variant];[Count]
	 * Where {...} is obligatory and [...] not */
	@SuppressWarnings("unchecked")
	private ItemStack getStackBySyntax(String itemSyntax, Player sender) {
		String[] itemSplit = itemSyntax.split(";");
		ItemType it = ItemTypes.NONE;
		int damage = 0;
		int count = 1;
		String variant = "";
		
		if((itemSplit.length >= 1 && itemSplit[0].contains(":")) || (!itemSyntax.contains(";") && itemSyntax.contains(":"))) {
			String[] itemID = itemSyntax.split(":");
			if(itemID[1].contains(";"))
				itemID[1] = itemID[1].split(";")[0];
			if(itemID.length > 2)
				QuestionsTime.getInstance().getLogger().warn("An item's id contains two or more \":\" (\""+itemSyntax+"\")");
			else if(itemID.length < 2)
				QuestionsTime.getInstance().getLogger().warn("An item's id contains only the mod's id or the name's item."
						+ " Delete the \":\" or add the mod's id / name's item (\""+itemSyntax+"\")");
			else {
				it = Sponge.getRegistry().getType(ItemType.class, (itemID[0]+":"+itemID[1])).orElse(ItemTypes.NONE);
				if(!itemID[1].equals("NONE") && it.getType().equals(ItemTypes.NONE))
					QuestionsTime.getInstance().getLogger().warn("The item's id (\""+itemID[1]+"\") doesn't exist");
			}
		} else {
			String itemID = itemSyntax.contains(";") ? itemSplit[0] : itemSyntax;
			it = Sponge.getRegistry().getType(ItemType.class, ("minecraft:"+itemID)).orElse(ItemTypes.NONE);
			if(!itemID.equals("NONE") && it.getType().equals(ItemTypes.NONE))
				QuestionsTime.getInstance().getLogger().warn("The item's id (\""+itemID+"\") doesn't exist");
		}
		if(itemSplit.length >= 2) {
			if(StringUtils.isNumeric(itemSplit[1])) {
				if(Integer.valueOf(itemSplit[1]) >= 0)
					damage = Integer.valueOf(itemSplit[1]);
				else
					QuestionsTime.getInstance().getLogger().warn("The items's damage is negative (\""+itemSyntax+"\" -> \""+itemSplit[1]+"\")");
			} else
				variant = itemSplit[1];
		}
		if(itemSplit.length >= 3) {
			if(StringUtils.isNumeric(itemSplit[2])) {
				if(Integer.valueOf(itemSplit[2]) >= 0)
					count = Integer.valueOf(itemSplit[2]);
				else
					QuestionsTime.getInstance().getLogger().warn("The items's count is negative (\""+itemSyntax+"\" -> \""+itemSplit[2]+"\")");
			} else
				QuestionsTime.getInstance().getLogger().warn("The item's count isn't an number (\""+itemSyntax+"\" -> \""+itemSplit[2]+"\")");
		}
		
		ItemStack is = ItemStack.builder().itemType(it).quantity(count).build();
		
		boolean variantExist = false;
		if(!variant.isEmpty() && QuestionsTime.getInstance().getSpongeAPI() == 7) {
			searchVariant: {
			for(@SuppressWarnings("rawtypes") Key key : Sponge.getRegistry().getAllOf(Key.class)) {
				if(CatalogType.class.isAssignableFrom(key.getElementToken().getRawType())) {
					for(CatalogType element : Sponge.getRegistry().getAllOf((Class<CatalogType>) key.getElementToken().getRawType())) {
						
						String elmtID = element.getId();
						if(elmtID.contains(":")) {
							if(elmtID.split(":").length >= 2 && !elmtID.split(":")[1].isEmpty())
								elmtID = elmtID.split(":")[1];
						}

						if(!elmtID.equals("none")) {
							if(elmtID.equals(variant)) {
								variantExist = true;
								if(is.supports(key)) {
									is.offer(key, element);
									break searchVariant;
								} else
									QuestionsTime.getInstance().getLogger().info("The variant \""+variant+"\" isn't applicable for the item \""
											+is.getType().getId()+"\" {\""+itemSyntax+"\" -> \""+itemSplit[1]+"\"}");
							}
						}
					}
				}
			}
			if(!variantExist)
				QuestionsTime.getInstance().getLogger().error("No variant named \""+variant+"\" has been found {\""+itemSyntax+"\" -> \""+itemSplit[1]+"\")");
			}
		} else if(damage > 0)
			is = ItemStack.builder().fromContainer(is.toContainer().set(DataQuery.of("UnsafeDamage"), damage)).build();
		return is;
	}
	
	@Listener
	public void onPlayerDisconnected(ClientConnectionEvent.Disconnect e) {
		QuestionsTime instance = QuestionsTime.getInstance();
		if(Sponge.getGame().getServer().getOnlinePlayers().size() == 1 && instance.getCurrentQuestion().isPresent()) {
			instance.setPlayedQuestion(Optional.empty());
			instance.getLogger().info("The last player connected has been disconnected while a question was said. The question has been stopped.");
			instance.sayNewQuestion();
		}
		if(instance.isCreator(e.getTargetEntity().getUniqueId())) {
			instance.removeCreator(e.getTargetEntity().getUniqueId());
			instance.stopTimer();
			instance.getLogger().info("The player's name "+e.getTargetEntity().getName()+" was creating a question when he disconnected. The question will not be save.");
		}
	}
	
	
	
}
