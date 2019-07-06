package fr.nocturne123.questionstime.message.component;

import org.spongepowered.api.text.Text;

public abstract class MessageComponent<T> {

    private final String name;

    public MessageComponent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract Text process(T type);

}
