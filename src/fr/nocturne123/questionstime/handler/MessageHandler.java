package fr.nocturne123.questionstime.handler;

import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.message.Message;
import fr.nocturne123.questionstime.util.TextUtils;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class MessageHandler {

    private static HashMap<String, TextTemplate> messages;
    private static ConsoleSource console = QuestionsTime.getInstance().getConsole();

    public static void init(Path path) {
        try {
            loadDefaultMessages();
            if (path != null) {
                console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("Loading messages..."));
                if (Files.notExists(Paths.get(path.toString(), "message.conf"))) {
                    QuestionsTime.getInstance().getContainer().getAsset("message.conf")
                            .ifPresent(asset -> {
                                try {
                                    asset.copyToDirectory(path, false, true);
                                    console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("The message file was not found, the default message file has been created"));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                } else
                    loadMessages(Paths.get(path.toString(), "message.conf"));
            } else {
                console.sendMessage(
                        TextUtils.Console.creatorNormalWithPrefix("The message.conf was not found, the default messages will be used"));
            }
        } catch (Exception e) {
            console.sendMessage(TextUtils.Console.creatorError("Error when loading the message.conf"));
            e.printStackTrace();
        }
    }

    private static void loadMessages(Path path) {
        int changedMessages = 0;
        ConfigurationLoader<CommentedConfigurationNode> loader = HoconConfigurationLoader.builder().setPath(path).build();
        CommentedConfigurationNode root;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for(Map.Entry<String, TextTemplate> entry : messages.entrySet()) {
            try {
                String section = entry.getKey();
                TextTemplate defaultValue = entry.getValue();
                CommentedConfigurationNode node = root.getNode((Object[]) section.split("\\."));
                String value = node.getString();
                if (value != null) {
                    if (!defaultValue.toText().toPlain().equals(value)) {
                        SortedSet<String> componentsDefault = new TreeSet<>(defaultValue.getArguments().keySet());
                        SortedSet<String> componentsText = new TreeSet<>(messageToTextTemplate(value).getArguments().keySet());
                        if (componentsDefault.equals(componentsText)) {
                            messages.put(section, messageToTextTemplate(value));
                            changedMessages++;
                        } else {
                            if (componentsDefault.isEmpty())
                                console.sendMessage(TextUtils.Console.creatorError("  The section \"" + section + "\" shall not have components"));
                            else if (componentsText.size() > componentsDefault.size())
                                console.sendMessage(TextUtils.Console.creatorError("  The section \"" + section + "\" have more component(s) that needed, he only need : " + componentsDefault));
                            else if (componentsText.size() == componentsDefault.size())
                                console.sendMessage(TextUtils.Console.creatorError("  The section \"" + section + "\" doesn't have the right component(s), he need : " + componentsDefault));
                            else if (componentsText.size() < componentsDefault.size())
                                console.sendMessage(TextUtils.Console.creatorError("  The section \"" + section + "\" have less component(s) that needed, he need : " + componentsDefault));
                        }
                    }
                } else
                    console.sendMessage(TextUtils.Console.creatorError("  Error when reading the text for the section \"" + section + "\". does it exist ?"));
            } catch (Exception e) {
                console.sendMessage(TextUtils.Console.creatorError("Error when reading \"messages.conf\""));
                e.printStackTrace();
            }
        }
        console.sendMessage(TextUtils.Console.creatorNormalWithPrefix("Messages loaded, " + changedMessages + "/" + messages.size() + " replaced"));
    }


    private static void loadDefaultMessages() {
        messages = new HashMap<>();
        for(Messages message : Messages.values())
            messages.put(message.section, messageToTextTemplate(message.message));
    }

    public static Text get(Messages message) {
        if (messages.containsKey(message.getSection())) {
            TextTemplate messageRegistered = messages.get(message.getSection());
            if (messageRegistered.getArguments().isEmpty())
                return Text.of(messageRegistered);
            else
                return Text.EMPTY;
        }
        return Text.EMPTY;
    }

    public static Text get(Message message) {
        if (messages.containsKey(message.getSection())) {
            TextTemplate messageRegistered = messages.get(message.getSection());
            int componentSize = messageRegistered.getArguments().size();
            if (message.getComponentSize() == componentSize)
                return message.getFormattedMessage();
            else
                QuestionsTime.getInstance().getLogger().warn("The message \""+message.getSection()+"\" required "+componentSize+" components but "+message.getComponentSize()+" was give,");
        }
        return Text.EMPTY;
    }

    private static TextTemplate messageToTextTemplate(String message) {
        String[] splitArg = message.split("(?=\\{)|(?<=})");
        Object[] template = new Object[splitArg.length];
        for(int i = 0; i < splitArg.length; i++) {
            String str = splitArg[i];
            if(str.startsWith("{") && str.endsWith("}"))
                template[i] = TextTemplate.arg(str.substring(1, str.length()-1)).build();
            else
                template[i] = str;
        }
        return TextTemplate.of(template);
    }

    public static TextTemplate getTextTemplate(Messages message) {
        return messages.get(message.getSection());
    }

    public enum Messages {

        QUESTION_NEW("question.new", "§eIt's Question Time !"),
        QUESTION_ASK("question.ask", "§e§l{question}"),
        QUESTION_PROPOSITION("question.proposition", "§b•{position}] {proposition}"),
        QUESTION_END("question.end", "§eMay the best win !"),
        QUESTION_TIMER_END("question.timer.end", "§eYou have §9§l{timer}§r§e to answer ! May the best win !"),
        QUESTION_TIMER_LEFT("question.timer.left", "§eYou have §9§l{timer}§r§e to answer !"),
        QUESTION_TIMER_OUT("question.timer.out", "§cNobody have found the answer, maybe a next time"),
        PRIZE_ANNOUNCE("prize.announce", "§eThe winner win :"),
        PRIZE_MONEY("prize.money", "§9•{money} §r{currency}"),
        PRIZE_ITEM("prize.item", "§9• {quantity} * {modid}§f{item} §b{metadata}"),//{customname} {lore}
        MALUS_ANNOUNCE("malus.announce", "§cBut a wrong answer :"),
        MALUS_MONEY("malus.money", "§4• -{money} §r{currency}"),
        ANSWER_ANNOUNCE("answer.announce", "§eAnswer with : \"§bqt>answer§e\""),
        ANSWER_WIN("answer.win", "§e§lYou win !"),
        ANSWER_WIN_ANNOUNCE("answer.win-announce", "§e§l{name} win !"),
        ANSWER_FALSE("answer.false", "§e§l{answer} §cisn't the right answer :("),
        ANSWER_MALUS("answer.malus", "§cYou lose §4{money} §r{currency}"),
        ANSWER_COOLDOWN("answer.cooldown", "§cYou have to wait {timer} to suggest an another answer"),
        REWARD_ANNOUNCE("reward.announce", "§e§lHere's your reward :"),
        REWARD_PRIZE("reward.prize", "§9• {quantity} * {modid}§f{item} §b{metadata}"),
        REWARD_MONEY("reward.money", "§9•{money} §r{currency}");

        private String section;
        private String message;

        Messages(String path, String message) {
            this.section = path;
            this.message = message;
        }

        public String getSection() {
            return section;
        }

        public String getMessage() {
            return message;
        }

    }

}
