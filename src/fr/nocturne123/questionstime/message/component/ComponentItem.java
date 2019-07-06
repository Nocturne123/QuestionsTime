package fr.nocturne123.questionstime.message.component;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.List;
import java.util.Optional;

public class ComponentItem extends MessageComponent<ItemStack> {

    public ComponentItem(String name) {
        super(name);
    }

    @Override
    public Text process(ItemStack itemStack) {
        Optional<Text> nameOpt = itemStack.get(Keys.DISPLAY_NAME);
        Optional<List<Text>> loreOpt = itemStack.get(Keys.ITEM_LORE);
        Text finalText = Text.EMPTY;
        Text hoverText = Text.EMPTY;
        if(loreOpt.isPresent()) {
            List<Text> loreTexts = loreOpt.get();
            if(!loreTexts.isEmpty()) {
                for(Text loreText : loreTexts)
                    hoverText = Text.join(hoverText, loreText, Text.NEW_LINE);
            }
        }
        if(nameOpt.isPresent()) {
            Text nameText = nameOpt.get();
            if(!nameText.isEmpty()) {
                if(!hoverText.isEmpty())
                    hoverText = Text.join(hoverText, Text.NEW_LINE, Text.builder(itemStack.getType().getName())
                            .color(TextColors.GRAY)
                            .style(TextStyles.ITALIC)
                            .build());
                else
                    hoverText = Text.builder(itemStack.getType().getName())
                            .color(TextColors.GRAY)
                            .style(TextStyles.ITALIC)
                            .build();
                finalText = Text.of(nameText);
                //finalText = Text.builder().append(nameText).style(TextStyles.UNDERLINE).build();
            }
        }
        if(loreOpt.isPresent() || nameOpt.isPresent())
            finalText = Text.builder().append(finalText).style(TextStyles.UNDERLINE).build();
        if(!hoverText.isEmpty())
            finalText = Text.builder().append(finalText).onHover(TextActions.showText(hoverText)).build();
        if(!finalText.isEmpty())
            return finalText;
        return Text.of(StringUtils.capitalize(itemStack.getType().getName().split(":")[1].replace('_', ' ')));
    }

}
