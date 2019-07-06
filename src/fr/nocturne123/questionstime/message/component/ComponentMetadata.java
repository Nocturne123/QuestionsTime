package fr.nocturne123.questionstime.message.component;

import fr.nocturne123.questionstime.QuestionsTime;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class ComponentMetadata extends MessageComponent<ItemStack> {

    public ComponentMetadata(String name) {
        super(name);
    }

    @Override
    public Text process(ItemStack itemStack) {
        Optional<Object> damageOpt = itemStack.toContainer().get(DataQuery.of("UnsafeDamage"));
        if(damageOpt.isPresent()) {
            Object obj = damageOpt.get();
            if(obj instanceof Integer)
                return Text.of((int) obj == 0 ? "" : String.valueOf(obj));
        } else
            QuestionsTime.getInstance().getLogger().warn("\"UnsafeDamage\" not found for the itemstack "+itemStack.getType().getName());
        return Text.EMPTY;
    }
}
