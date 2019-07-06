package fr.nocturne123.questionstime.message.component;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class ComponentPlayer extends MessageComponent<Player> {

    public ComponentPlayer(String name) {
        super(name);
    }

    @Override
    public Text process(Player player) {
        return Text.of(player.getName());
    }
}
