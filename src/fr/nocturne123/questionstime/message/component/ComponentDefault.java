package fr.nocturne123.questionstime.message.component;

import org.spongepowered.api.text.Text;

public class ComponentDefault<T> extends MessageComponent<T> {

    public ComponentDefault(String name) {
        super(name);
    }

    @Override
    public Text process(T type) {
        return Text.of(String.valueOf(type));
    }
}
