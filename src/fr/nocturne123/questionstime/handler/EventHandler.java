package fr.nocturne123.questionstime.handler;

import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.message.Message;
import fr.nocturne123.questionstime.message.MessageComponents;
import fr.nocturne123.questionstime.question.Question;
import fr.nocturne123.questionstime.question.Question.Types;
import fr.nocturne123.questionstime.question.QuestionCreator;
import fr.nocturne123.questionstime.question.component.Malus;
import fr.nocturne123.questionstime.question.component.Prize;
import fr.nocturne123.questionstime.question.component.PrizeSerializer;
import fr.nocturne123.questionstime.util.TextUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class EventHandler {

    private Map<UUID, Long> playerCooldownAnswer = new HashMap<>();

    @Listener
    public void onReceiveMessage(MessageChannelEvent event) {
        if (event.getCause() != null && event.getCause().first(Player.class).isPresent() && !event.getCause().containsType(PluginContainer.class)) {
            QuestionsTime instance = QuestionsTime.getInstance();
            Player player = event.getCause().first(Player.class).get();
            String message = event.getMessage().toPlain();
            if (message.endsWith("qtc>start") && !instance.isCreator(player.getUniqueId())) {
                instance.addCreator(player.getUniqueId());
                this.handleQuestionCreation(message, player, instance, event);
                return;
            }
            if (message.contains("qt>") && !instance.isCreator(player.getUniqueId()) && instance.getCurrentQuestion().isPresent())
                this.handleQuestionAnswer(message, player, instance, event);
            if (message.contains("qtc>") && instance.isCreator(player.getUniqueId()))
                this.handleQuestionCreation(message, player, instance, event);
        }
    }

    private void handleQuestionAnswer(String message, Player sender, QuestionsTime instance, MessageChannelEvent event) {
        Question question = instance.getCurrentQuestion().get();
        if (this.playerCooldownAnswer.containsKey(sender.getUniqueId())) {
            long time = this.playerCooldownAnswer.get(sender.getUniqueId());
            if (System.currentTimeMillis() > time)
                this.playerCooldownAnswer.remove(sender.getUniqueId());
            else {
                sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.ANSWER_COOLDOWN)
                        .setComponent(MessageComponents.TIMER, (int) (this.playerCooldownAnswer.get(sender.getUniqueId()) - System.currentTimeMillis()) / 1000)
                        .build())));
                return;
            }
        }
        if (message.endsWith("qt>" + question.getAnswer())) {
            QuestionsTime.getInstance().setPlayedQuestion(null);
            if (question.isTimed()) instance.stopTimer();
            Optional<Prize> prizeOptional = question.getPrize();
            Task.builder().execute(wait -> Sponge.getServer().getOnlinePlayers().forEach(player -> {
                if (player.getUniqueId().equals(player.getUniqueId()))
                    player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.ANSWER_WIN)));
                else
                    player.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.ANSWER_WIN_ANNOUNCE).setComponent(MessageComponents.PLAYER_NAME, player).build())));
            })).async().delay(500, TimeUnit.MILLISECONDS)
                    .submit(instance.getContainer().getInstance().get());

            prizeOptional.ifPresent(prize -> {
                if (prize.getItemStacks().length > 0 || (prize.getMoney() > 0 && instance.getEconomy().isPresent()))
                    Task.builder().execute(task -> {
                        sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(MessageHandler.Messages.REWARD_ANNOUNCE)));
                        if (prize.getItemStacks().length > 0) {
                            for (int i = 0; i < prize.getItemStacks().length; i++) {
                                ItemStack item = prize.getItemStacks()[i];
                                sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.REWARD_PRIZE)
                                        .setComponent(MessageComponents.QUANTITY, item.getQuantity())
                                        .setComponent(MessageComponents.MOD_ID, item)
                                        .setComponent(MessageComponents.ITEM, item)
                                        .setComponent(MessageComponents.METADATA, item)
                                        .build())));
                                sender.getInventory().offer(prize.getItemStacks()[i].copy());
                            }
                        }
                        if (prize.getMoney() > 0 && instance.getEconomy().isPresent()) {
                            EconomyService ecoSevice = instance.getEconomy().get();
                            sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.REWARD_MONEY)
                                    .setComponent(MessageComponents.MONEY, prize.getMoney())
                                    .setComponent(MessageComponents.CURRENCY, ecoSevice)
                                    .build())));
                            Optional<UniqueAccount> account = ecoSevice.getOrCreateAccount(sender.getUniqueId());
                            if (account.isPresent())
                                account.get().deposit(ecoSevice.getDefaultCurrency(), BigDecimal.valueOf(prize.getMoney()),
                                        event.getCause());
                            else
                                instance.getLogger().error("The economy account for " + sender.getName() + " can't be found / created.");
                        } else if (!instance.getEconomy().isPresent())
                            instance.getLogger().info("No Economy Service found.");
                    }).async()
                            .delayTicks(60)
                            .name("[QT]SendWinnerPrize")
                            .submit(instance.getContainer().getInstance().get());
            });

            instance.sayNewQuestion();
        } else if (message.contains("qt>")) {
            String answer = message.substring(message.lastIndexOf("qt>") + 3);
            sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.ANSWER_FALSE)
                    .setComponent(MessageComponents.ANSWER, answer).build())));
            if (ConfigHandler.isPersonalAnswer())
                event.setMessageCancelled(true);
            if (question.isTimeBetweenAnswer())
                this.playerCooldownAnswer.put(sender.getUniqueId(), System.currentTimeMillis() + (question.getTimeBetweenAnswer() * 1000));
            Optional<Malus> malusOptional = question.getMalus();
            malusOptional.ifPresent(malus -> {
                if (malus.getMoney() > 0 && instance.getEconomy().isPresent()) {
                    EconomyService economyService = instance.getEconomy().get();
                    sender.sendMessage(Text.join(instance.qtPrefix, MessageHandler.get(Message.builder(MessageHandler.Messages.ANSWER_MALUS)
                            .setComponent(MessageComponents.MONEY, malus.getMoney())
                            .setComponent(MessageComponents.CURRENCY, economyService).build())));
                    Optional<UniqueAccount> account = economyService.getOrCreateAccount(sender.getUniqueId());
                    if (account.isPresent())
                        account.get().withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(malus.getMoney()), event.getCause());
                    else
                        instance.getLogger().error("The economy account for " + sender.getName() + " can't be found / created.");
                } else if (!instance.getEconomy().isPresent())
                    instance.getLogger().info("No Economy Service found.");
            });
        }
    }

    private void handleQuestionCreation(String message, Player p, QuestionsTime instance, MessageChannelEvent e) {
        if (instance.getQuestionCreator(p.getUniqueId()).isPresent()) {
            QuestionCreator qc = instance.getQuestionCreator(p.getUniqueId()).get();
            String answer = message.substring(message.lastIndexOf("qtc>") + 4);
            if (answer.equals("stop"))
                qc.setStop();
            if (answer.equals("confirm") && qc.isConfirm())
                qc.nextStep();
            else
                qc.setPreviousResponse(answer);

            switch (qc.getCurrentStep()) {
                case -1:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to quit the Question Creator ? The question will not be saved !"));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to continue the creation of the command ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer need to be \"", "qtc>yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to stop the Question Creator ?"));
                        p.sendMessage(TextUtils.creatorComposed("Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 0:
                    if (qc.isHalfConfirm()) {
                        p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right question ?"));
                        qc.setConfirm();
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("What's the question ? Answer with \"", "qtc>question", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 1:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("simple")) {
                            p.sendMessage(TextUtils.creatorComposed("The question's type is \"", "simple", "\". That's right ?"));
                            qc.setConfirm();
                        } else if (answer.equals("proposition")) {
                            p.sendMessage(TextUtils.creatorComposed("The question's type is \"", "proposition", "\". That's right ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The question's type can only be \"", "simple OR proposition", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(Text.join(TextUtils.creatorComposed("What's the question's type ? Answer with \"", "qtc>simple", "\" or \""),
                                Text.builder("qtc>proposition").color(TextColors.BLUE).build(),
                                Text.builder("\"").color(TextColors.GREEN).build()));
                        qc.setHalfConfirm();
                    }
                    break;
                case 2:
                    if (qc.isHalfConfirm()) {
                        String[] split = answer.split(" ", 2);
                        if (split.length == 2 && !split[0].isEmpty() && !split[1].isEmpty()) {
                            String action = split[0];
                            String argument = split[1];
                            if ("add".equals(action)) {
                                if (argument.contains(";")) {
                                    List<String> listProposition = Arrays.asList(argument.split(";"));
                                    listProposition.removeIf(String::isEmpty);
                                    if (!listProposition.isEmpty()) {
                                        if (qc.getPropositions().size() + listProposition.size() < 128) {
                                            for (int i = 0; i < listProposition.size(); i++) {
                                                String preposition = listProposition.get(i);
                                                p.sendMessage(TextUtils.creatorComposed("", "[" + (qc.getPropositions().size() + i + 1) + "] " + preposition, " added"));
                                            }
                                            qc.getPropositions().addAll(listProposition);
                                        } else
                                            p.sendMessage(TextUtils.creatorComposed("The number of proposition will exceed ", String.valueOf(127), ""));
                                    } else
                                        p.sendMessage(TextUtils.creatorNormalWithPrefix("You wrote empty prepositions"));
                                } else {
                                    if (qc.getPropositions().size() < 128) {
                                        qc.getPropositions().add(argument);
                                        p.sendMessage(TextUtils.creatorComposed("Proposition ", "[" + qc.getPropositions().size() + "] " + argument, " added"));
                                    } else
                                        p.sendMessage(TextUtils.creatorComposed("The number of proposition will exceed ", String.valueOf(127), ""));
                                }
                            } else if ("set".equals(action)) {
                                String[] split2 = argument.split(" ", 2);
                                if (split2.length >= 2 && !split2[0].isEmpty() && !split2[1].isEmpty()) {
                                    if (StringUtils.isNumeric(split2[0])) {
                                        int position = Integer.parseInt(split2[0]);
                                        if (position > 0 && position <= qc.getPropositions().size()) {
                                            qc.getPropositions().set(position - 1, split2[1]);
                                            p.sendMessage(TextUtils.creatorComposed("Proposition ", "[" + position + "] " + split2[1], " modified !"));
                                        } else {
                                            if (qc.getPropositions().isEmpty())
                                                p.sendMessage(TextUtils.creatorNormalWithPrefix("No propositions are registered"));
                                            else if (qc.getPropositions().size() == 1)
                                                p.sendMessage(Text.join(TextUtils.creatorComposed("You can only set the proposition ", String.valueOf(1), "")));
                                            else
                                                p.sendMessage(Text.join(TextUtils.creatorComposed("The position need to be bewteen ", "1", " and "), TextUtils.creatorSpecial(String.valueOf(qc.getPropositions().size()))));
                                        }
                                    } else
                                        p.sendMessage(TextUtils.creatorComposed("\"", split2[0], "\" is not a number"));
                                } else
                                    p.sendMessage(TextUtils.creatorComposed("Arguments ", argument, " are empty or less than 2"));
                            } else if ("del".equals(action)) {
                                if (StringUtils.isNumeric(argument)) {
                                    int position = Integer.parseInt(argument);
                                    if (position > 0 && position <= qc.getPropositions().size()) {
                                        qc.getPropositions().remove(position - 1);
                                        p.sendMessage(TextUtils.creatorComposed("Proposition ", String.valueOf(position), " deleted !"));
                                    } else
                                        p.sendMessage(Text.join(TextUtils.creatorComposed("The position need to be bewteen ", "1", " and "), TextUtils.creatorSpecial(String.valueOf(qc.getPropositions().size()))));
                                } else p.sendMessage(TextUtils.creatorComposed("\"", argument, "\" is not a number"));
                            } else
                                p.sendMessage(TextUtils.creatorComposed("The first word need to be \"", "add OR set OR del OR list", "\""));
                        } else if ("list".equals(answer)) {
                            if (qc.getPropositions().size() == 0)
                                p.sendMessage(TextUtils.creatorNormalWithPrefix("No propositions have been made"));
                            else {
                                for (int i = 0; i < qc.getPropositions().size(); i++)
                                    p.sendMessage(TextUtils.creatorComposed("", (i + 1) + "] ", qc.getPropositions().get(i)));
                            }
                        } else
                            p.sendMessage(TextUtils.creatorComposed("Answer \"", answer, "\" not recognized. Did you add all the arguments required ?"));
                        if (qc.getPropositions().size() >= 2 && qc.getPropositions().size() <= 127)
                            qc.setConfirm();
                        else {
                            qc.setHalfConfirm();
                            if ("confirm".equals(answer))
                                p.sendMessage(TextUtils.creatorComposed("You need to write ", String.valueOf(2), " propositions at least"));
                        }

                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Write a proposition with \"", "qtc>add proposition", "\""));
                        p.sendMessage(TextUtils.creatorComposed("You can separate each proposition with a \"", ";", "\""));
                        p.sendMessage(TextUtils.creatorComposed("Modify a proposition with \"", "qtc>set position proposition", "\""));
                        p.sendMessage(TextUtils.creatorComposed("Delete a proposition with \"", "qtc>del position", "\""));
                        p.sendMessage(TextUtils.creatorComposed("You can list the propositions with \"", "qtc>list", "\""));
                        p.sendMessage(Text.join(TextUtils.creatorComposed("The number of proposition need to be between ", "2", " and "), TextUtils.creatorSpecial("127")));
                        qc.setHalfConfirm();
                    }
                    break;
                case 3:
                    if (qc.isConfirm() || qc.isHalfConfirm()) {
                        if (qc.getQuestionType() == Types.MULTI) {
                            if (StringUtils.isNumeric(answer)) {
                                int position = Integer.parseInt(answer);
                                if (position <= qc.getPropositions().size() && position >= 0) {
                                    p.sendMessage(TextUtils.creatorComposed("The right proposition is \"", answer, "\". It is correct ?"));
                                    qc.setConfirm();
                                } else {
                                    p.sendMessage(Text.join(TextUtils.creatorComposed("The right proposition can only be a number between ", "0", " and "), TextUtils.creatorSpecial(String.valueOf(qc.getPropositions().size()))));
                                    qc.setHalfConfirm();
                                }
                            } else {
                                p.sendMessage(TextUtils.creatorNormalWithPrefix("The right proposition can only be a number"));
                                qc.setHalfConfirm();
                            }
                        } else if (qc.getQuestionType() == Types.SIMPLE) {
                            p.sendMessage(TextUtils.creatorComposed("The answer is \"", answer, "\". It is correct ?"));
                            qc.setConfirm();
                        }

                    } else if (qc.getQuestionType() == Types.MULTI) {
                        p.sendMessage(TextUtils.creatorComposed("What's the right proposition ? Answer with \"", "qtc>number", "\""));
                        qc.setHalfConfirm();
                    } else if (qc.getQuestionType() == Types.SIMPLE) {
                        p.sendMessage(TextUtils.creatorComposed("What's the answer of the question ? Answer with \"", "qtc>answer", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 4:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to add prizes ?")));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add prizes ?")));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to add prizes ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 5:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to announce the prize after the question was asked ?")));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really want to not announce the prize after the question was asked ?")));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to announce the prize ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 6:
                    if (qc.isHalfConfirm()) {
                        if (StringUtils.isNumeric(answer)) {
                            if (answer.length() <= 18) {
                                if (Long.valueOf(answer) > 0) {
                                    p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right amount ?"));
                                    qc.setConfirm();
                                } else {
                                    p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add money as a prize ?")));
                                    qc.setConfirm();
                                }
                            } else {
                                p.sendMessage(TextUtils.creatorComposed("\"", answer, "\" is too big !"));
                                qc.setHalfConfirm();
                            }
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("\"", answer, "\" isn't a number or a positive number"));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(Text.join(TextUtils.creatorComposed("What is the amount of ",
                                instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain(), " ? Answer with \""), TextUtils.creatorSpecial("qtc>amount"),
                                TextUtils.creatorNormal("\" (if you doesn't want, just put 0)")));
                        qc.setHalfConfirm();
                    }
                    break;
                case 7:
                    if (qc.isConfirm()) {
                        ItemStack is = PrizeSerializer.getStackBySyntax(answer);
                        if (is.getType() != ItemTypes.NONE) {
                            qc.addItemPrize(is);
                            p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" Added \""), TextUtils.readableItemID(is),
                                    Text.builder(" * " + is.getQuantity()).color(TextColors.LIGHT_PURPLE).build(),
                                    TextUtils.creatorNormal("\"")));
                        } else
                            p.sendMessage(TextUtils.creatorComposed("Incorrect syntax : \"", answer, "\""));
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Add an item as prize with \"", "qtc>[ModID]:{ItemID};[Variant];[Count];[DisplayName];[Lore]", "\""));
                        p.sendMessage(TextUtils.creatorComposed("Which \"", "{...}", "\" is obligatory"));
                        p.sendMessage(TextUtils.creatorComposed("And \"", "[...]", "\" optionnal"));
                        p.sendMessage(TextUtils.creatorComposed("If you don't want to add items, type \"", "qtc>confirm", "\" directly"));
                        qc.setConfirm();
                    }
                    break;
                case 8:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to add a malus ?"));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not add a malus ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to add a malus for a wrong answer ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 9:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to announce the malus ?"));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not announce the malus ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to announce the malus after the question ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 10:
                    if (qc.isHalfConfirm()) {
                        if (StringUtils.isNumeric(answer)) {
                            if (answer.length() <= 18) {
                                if (Long.valueOf(answer) > 0) {
                                    p.sendMessage(TextUtils.creatorComposed("Is \"", answer, "\" the right amount ?"));
                                    qc.setConfirm();
                                } else {
                                    p.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorNormal(" You really don't want to add money as a malus ?")));
                                    qc.setConfirm();
                                }
                            } else {
                                p.sendMessage(TextUtils.creatorComposed("\"", answer, "\" is too big !"));
                                qc.setHalfConfirm();
                            }
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("\"", answer, "\" isn't a number or a positive number"));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(Text.join(TextUtils.creatorComposed("What is the amount of ",
                                instance.getEconomy().get().getDefaultCurrency().getDisplayName().toPlain(), " ? Answer with \""), TextUtils.creatorSpecial("qtc>amount"),
                                TextUtils.creatorNormal("\" (if you doesn't want, just put 0)")));
                        qc.setHalfConfirm();
                    }
                    break;
                case 11:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to add a timer ?"));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not add a timer ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to add a timer ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 12:
                    if (qc.isHalfConfirm())
                        this.handleTime(answer, p, qc, instance);
                    else {
                        p.sendMessage(Text.join(TextUtils.creatorComposed("How long do players have to wait between each answer ? Answer with this format \"", "qtc>xhxmxs", "\", where "),
                                TextUtils.creatorSpecial("x"), TextUtils.creatorNormal(" is a number")));
                        p.sendMessage(Text.join(TextUtils.creatorComposed("Note : the maximum is ", "23h59m59s", " and the minimum is "), TextUtils.creatorSpecial("0h0m10s")));
                        qc.setHalfConfirm();
                    }
                    break;
                case 13:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("yes")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to add a time between an answer ?"));
                            qc.setConfirm();
                        } else if (answer.equals("no")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to not add a time between an answer ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "yes OR no", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Do you want to add a time between an answer ? Answer with \"", "qtc>yes OR no", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
                case 14:
                    if (qc.isHalfConfirm())
                        this.handleTime(answer, p, qc, instance);
                    else {
                        p.sendMessage(Text.join(TextUtils.creatorComposed("How long the players can answer ? Answer with this format \"", "qtc>xhxmxs", "\", where "),
                                TextUtils.creatorSpecial("x"), TextUtils.creatorNormal(" is a number")));
                        p.sendMessage(Text.join(TextUtils.creatorComposed("Note : the maximum is ", "23h59m59s", " and the minimum is "), TextUtils.creatorSpecial("0h0m10s")));
                        qc.setHalfConfirm();
                    }
                    break;
                case 15:
                    if (qc.isHalfConfirm()) {
                        if (answer.equals("start")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to start the question ?"));
                            qc.setConfirm();
                        } else if (answer.equals("save")) {
                            p.sendMessage(TextUtils.creatorNormalWithPrefix("Do you really want to just save the question ?"));
                            qc.setConfirm();
                        } else {
                            p.sendMessage(TextUtils.creatorComposed("The answer can only be \"", "start OR save", "\""));
                            qc.setHalfConfirm();
                        }
                    } else {
                        p.sendMessage(TextUtils.creatorComposed("Good, it's finish ! The question is now registered ! Do you want to start the question or just save it ? "
                                + "Answer with \"", "qtc>start OR save", "\""));
                        qc.setHalfConfirm();
                    }
                    break;
            }
            e.setMessageCancelled(true);
        } else
            instance.getLogger().error("I think a spacetime error occurred because this is -normally- impossible to happen. But, yea, I think you found a bug. It is cool ?");
    }

    /**
     * Syntax : [ModID]:{ItemID};[Variant];[Count];[DisplayName];[Lore]
     * Where {...} is obligatory and [...] not
     *//*
    @SuppressWarnings("unchecked")
    private ItemStack getStackBySyntax(String itemSyntax, Player sender) {
        Logger logger = QuestionsTime.getInstance().getLogger();
        String[] itemSplit = itemSyntax.split(";");
        ItemType it = ItemTypes.NONE;
        int damage = 0;
        int count = 1;
        String variant = "";
        Text customName = Text.EMPTY;
        List<Text> lore = new ArrayList<>();

        if ((itemSplit.length >= 1 && itemSplit[0].contains(":")) || (!itemSyntax.contains(";") && itemSyntax.contains(":"))) {
            String[] itemID = itemSyntax.split(":");
            if (itemID[1].contains(";"))
                itemID[1] = itemID[1].split(";")[0];
            if (itemID.length > 2)
                logger.warn("An item's id contains two or more \":\" (\"" + itemSyntax + "\")");
            else if (itemID.length < 2)
                logger.warn("An item's id contains only the mod's id or the name's item."
                        + " Delete the \":\" or add the mod's id / name's item (\"" + itemSyntax + "\")");
            else {
                it = Sponge.getRegistry().getType(ItemType.class, (itemID[0] + ":" + itemID[1])).orElse(ItemTypes.NONE);
                if (!itemID[1].equals("NONE") && it.getType().equals(ItemTypes.NONE))
                    logger.warn("The item's id (\"" + itemID[1] + "\") doesn't exist");
            }
        } else {
            String itemID = itemSyntax.contains(";") ? itemSplit[0] : itemSyntax;
            it = Sponge.getRegistry().getType(ItemType.class, ("minecraft:" + itemID)).orElse(ItemTypes.NONE);
            if (!itemID.equals("NONE") && it.getType().equals(ItemTypes.NONE))
                logger.warn("The item's id (\"" + itemID + "\") doesn't exist");
        }
        if (itemSplit.length >= 2) {
            if (StringUtils.isNumeric(itemSplit[1])) {
                if (Integer.valueOf(itemSplit[1]) >= 0)
                    damage = Integer.valueOf(itemSplit[1]);
                else
                    logger.warn("The items's damage is negative (\"" + itemSyntax + "\" -> \"" + itemSplit[1] + "\")");
            } else
                variant = itemSplit[1];
        }
        if (itemSplit.length >= 3) {
            if (StringUtils.isNumeric(itemSplit[2])) {
                if (Integer.valueOf(itemSplit[2]) >= 0)
                    count = Integer.valueOf(itemSplit[2]);
                else
                    logger.warn("The items's count is negative (\"" + itemSyntax + "\" -> \"" + itemSplit[2] + "\")");
            } else
                logger.warn("The item's count isn't an number (\"" + itemSyntax + "\" -> \"" + itemSplit[2] + "\")");
        }
        if(itemSplit.length >= 4) {
            if(!itemSplit[3].isEmpty())
                customName = Text.of(itemSplit[3]);
            else
                logger.warn("The item's name is empty (\""+itemSyntax+"\" -> \""+itemSplit[3]+"\")");
        }
        if (itemSplit.length >= 5) {
            if (!itemSplit[4].isEmpty()) {
                String loreOneLine = itemSplit[4];
                if (loreOneLine.contains("\n")) {
                    for (String line : loreOneLine.split("\n"))
                        lore.add(Text.of(line));
                } else
                    lore.add(Text.of(loreOneLine));
            } else
                logger.warn("The item's lore is empty (\"" + itemSyntax + "\" -> \"" + itemSplit[4] + "\")");
        }

        ItemStack.Builder isBuilder = ItemStack.builder().itemType(it).quantity(count);
        if(!customName.isEmpty())
            isBuilder.add(Keys.DISPLAY_NAME, Text.of(customName));
        if(!lore.isEmpty())
            isBuilder.add(Keys.ITEM_LORE, lore).build();
        ItemStack is = isBuilder.build();

        boolean variantExist = false;
        if (!variant.isEmpty() && QuestionsTime.getInstance().getSpongeAPI() == 7) {
            searchVariant:
            {
                for (@SuppressWarnings("rawtypes") Key key : Sponge.getRegistry().getAllOf(Key.class)) {
                    if (CatalogType.class.isAssignableFrom(key.getElementToken().getRawType())) {
                        for (CatalogType element : Sponge.getRegistry().getAllOf((Class<CatalogType>) key.getElementToken().getRawType())) {

                            String elmtID = element.getId();
                            if (elmtID.contains(":")) {
                                if (elmtID.split(":").length >= 2 && !elmtID.split(":")[1].isEmpty())
                                    elmtID = elmtID.split(":")[1];
                            }

                            if (!elmtID.equals("none")) {
                                if (elmtID.equals(variant)) {
                                    variantExist = true;
                                    if (is.supports(key)) {
                                        is.offer(key, element);
                                        break searchVariant;
                                    } else
                                        QuestionsTime.getInstance().getLogger().info("The variant \"" + variant + "\" isn't applicable for the item \""
                                                + is.getType().getId() + "\" {\"" + itemSyntax + "\" -> \"" + itemSplit[1] + "\"}");
                                }
                            }
                        }
                    }
                }
                if (!variantExist)
                    QuestionsTime.getInstance().getLogger().error("No variant named \"" + variant + "\" has been found {\"" + itemSyntax + "\" -> \"" + itemSplit[1] + "\")");
            }
        } else if (damage > 0)
            is = ItemStack.builder().fromContainer(is.toContainer().set(DataQuery.of("UnsafeDamage"), damage)).build();
        return is;
    }*/
    private void handleTime(String answer, Player player, QuestionCreator qc, QuestionsTime instance) {
        if (Pattern.matches("^[0-9]+(h)[0-9]+(m)[0-9]+(s)$", answer)) {
            String timeHour = StringUtils.substringBefore(answer, "h");
            String timeMinute = StringUtils.substringBetween(answer, "h", "m");
            String timeSecond = StringUtils.substringBetween(answer, "m", "s");
            if (StringUtils.isNumeric(timeHour) && StringUtils.isNumeric(timeMinute) && StringUtils.isNumeric(timeSecond)) {
                int hour = Integer.valueOf(timeHour);
                int min = Integer.valueOf(timeMinute);
                int sec = Integer.valueOf(timeSecond);
                if (hour >= 0 && hour <= 23) {
                    if (min >= 0 && min <= 59) {
                        if (hour == 0 && min == 0 && sec < 10) {
                            player.sendMessage(TextUtils.creatorNormalWithPrefix("The time need to be at least 10 seconds"));
                            qc.setHalfConfirm();
                        } else if (sec >= 0 && sec <= 59) {
                            player.sendMessage(TextUtils.creatorComposed("Is the time ", hour + "H" + min + "M" + sec + "S", " enough to answer the question ?"));
                            qc.setConfirm();
                        } else {
                            player.sendMessage(TextUtils.creatorComposed("", timeSecond, " need to be between 0 and 59"));
                            qc.setHalfConfirm();
                        }
                    } else {
                        player.sendMessage(TextUtils.creatorComposed("", timeMinute, " need to be between 0 and 59"));
                        qc.setHalfConfirm();
                    }
                } else {
                    player.sendMessage(TextUtils.creatorComposed("", timeHour, " need to be between 0 and 23"));
                    qc.setHalfConfirm();
                }
            } else {
                if (!StringUtils.isNumeric(timeHour))
                    player.sendMessage(TextUtils.creatorComposed("", timeHour, " isn't an number"));
                if (!StringUtils.isNumeric(timeMinute))
                    player.sendMessage(TextUtils.creatorComposed("", timeMinute, " isn't an number"));
                if (!StringUtils.isNumeric(timeSecond))
                    player.sendMessage(TextUtils.creatorComposed("", timeSecond, " isn't an number"));
                qc.setHalfConfirm();
            }
        } else {
            player.sendMessage(Text.join(instance.qtPrefix, TextUtils.creatorSpecial(" " + answer), TextUtils.creatorNormal(" doesn't matche "),
                    TextUtils.creatorSpecial("xhxmxs")));
            qc.setHalfConfirm();
        }
    }

    @Listener
    public void onPlayerDisconnected(ClientConnectionEvent.Disconnect e) {
        QuestionsTime instance = QuestionsTime.getInstance();
        if (Sponge.getGame().getServer().getOnlinePlayers().size() == 1 && instance.getCurrentQuestion().isPresent()) {
            instance.setPlayedQuestion(null);
            instance.getLogger().info("The last player connected has been disconnected while a question was said. The question has been stopped.");
            instance.sayNewQuestion();
        }
        if (instance.isCreator(e.getTargetEntity().getUniqueId())) {
            instance.removeCreator(e.getTargetEntity().getUniqueId());
            instance.stopTimer();
            instance.getLogger().info("The player's name " + e.getTargetEntity().getName() + " was creating a question when he disconnected. The question will not be save.");
        }
    }

    /*@Listener
    public void onPlayerConnect(ClientConnectionEvent.Join event) {
        String test = "&aHello &e{player}&a !";
        Text text = TextSerializers.FORMATTING_CODE.deserialize(test);
        TextTemplate template = TextTemplate.of(text);
        //TextTemplate template = this.messageToTextTemplate(test);
        HashMap<String, Text> args = new HashMap<>();
        args.put("player", Text.builder(event.getTargetEntity().getName()).onHover(TextActions.showText(Text.of("Test"))).build());
        Text finalText = template.apply(args).build();
        event.getTargetEntity().sendMessage(finalText);
        //event.getTargetEntity().sendMessage(Text.of(finalText.toPlain()));
    }*/



}
