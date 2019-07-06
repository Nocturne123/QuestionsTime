package fr.nocturne123.questionstime.message;

import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.handler.MessageHandler;
import fr.nocturne123.questionstime.message.component.MessageComponent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;

import java.util.HashMap;
import java.util.Map;

public class Message {

    private MessageHandler.Messages message;
    private Map<MessageComponent, Object> components;

    private Message(MessageBuilder builder) {
        this.components = builder.components;
        this.message = builder.message;
    }

    public Text getFormattedMessage() {
        TextTemplate textTemplate = MessageHandler.getTextTemplate(message);
        //QuestionsTime.getInstance().getConsole().sendMessage(textTemplate.toText());
        HashMap<String, Text> args = new HashMap<>();
        for(Map.Entry<MessageComponent, Object> entry : this.components.entrySet()) {
            MessageComponent component = entry.getKey();
            Text text = component.process(entry.getValue());
            args.put(component.getName(), text);
           /* MessageComponent component = entry.getKey();
            Text textBefore = Text.of(StringUtils.substringBefore(message.toPlain(), "{"+component.getName()+"}"));
            Text textAfter = Text.of(StringUtils.substringAfter(message.toPlain(), "{"+component.getName()+"}"));
            Text newText = component.process(entry.getValue());
            message = Text.join(textBefore, newText, textAfter);*/
            //message = message.toPlain().replace("{"+component.getName()+"}", component.process(entry.getValue()));
        }
        //QuestionsTime.getInstance().getConsole().sendMessage(textTemplate.apply(args).build());
        //System.out.println(args);
        //System.out.println(textTemplate.toText().toPlain());
        return Text.of(textTemplate.apply(args).build().toPlain());
    }

    public String getSection() {
        return this.message.getSection();
    }

    public int getComponentSize() {
        return this.components.size();
    }

    public static MessageBuilder builder(MessageHandler.Messages message) {
        return new MessageBuilder(message);
    }

    public static class MessageBuilder {

        private MessageHandler.Messages message;
        private Map<MessageComponent, Object> components = new HashMap<>();

        public MessageBuilder(MessageHandler.Messages message) {
            this.message = message;
        }

        public <T> MessageBuilder setComponent(MessageComponent<T> component, T value) {
            if(value != null && component != null)
                this.components.put(component, value);
            else
                QuestionsTime.getInstance().getLogger().warn("The component or his value is/are null or empty {component="+component+",value="+value+"}");
            return this;
        }

        public Message build() {
            return new Message(this);
        }

    }

}
