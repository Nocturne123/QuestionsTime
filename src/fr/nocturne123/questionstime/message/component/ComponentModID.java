package fr.nocturne123.questionstime.message.component;

import fr.nocturne123.questionstime.QuestionsTime;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class ComponentModID extends MessageComponent<ItemStack> {

    public ComponentModID(String name) {
        super(name);
    }

    @Override
    public Text process(ItemStack itemStack) {
        String modid = itemStack.getType().getId().split(":")[0];
        if (modid.startsWith("minecraft"))
            return Text.EMPTY;
        else {
            Optional<PluginContainer> pluginCont = Sponge.getPluginManager().getPlugin(modid);
            if (pluginCont.isPresent()) {
                return Text.of(pluginCont.get().getName());
            } else {
                QuestionsTime.getInstance().getLogger().warn("The mod id "+modid+" has not been found");
                return Text.of(modid);
            }
        }
    }
}
