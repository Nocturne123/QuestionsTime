package fr.nocturne123.questionstime.question.component;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fr.nocturne123.questionstime.QuestionsTime;
import fr.nocturne123.questionstime.util.TextUtils;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.data.manipulator.mutable.item.LoreData;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;

public class PrizeSerializer implements TypeSerializer<Prize> {

    private static final ConsoleSource console = QuestionsTime.getInstance().getConsole();

    @Nullable
    @Override
    public Prize deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode node) {
        int money = node.getNode("money").getInt(0);
        boolean announce = node.getNode("announce").getBoolean(true);
        ArrayList<ItemStack> isList = new ArrayList<>();

        ConfigurationNode items = node.getNode("items");
        if (items.getValue() != null) {
            items.getChildrenList().forEach(preItemNode -> {
                String preItem = preItemNode.getString();
                try {
                    if(preItem != null) {
                        ItemStack is = getStackBySyntax(preItem);
                        if (!is.getType().getType().equals(ItemTypes.NONE))
                            isList.add(is);
                        else
                            console.sendMessage(TextUtils.Console.creatorError("  Error when convert \"" + preItem + "\" to an itemstack, or the item type is none"));
                    } else
                        console.sendMessage(TextUtils.Console.creatorError("  The string is null"));
                } catch (Exception e) {
                    console.sendMessage(TextUtils.Console.creatorError("  Error when loading an item {\"" + preItem + "\"}"));
                    e.printStackTrace();
                }
            });
        }
        return new Prize(money, announce, isList.toArray(new ItemStack[0]));
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable Prize prize, @NonNull ConfigurationNode node) throws ObjectMappingException {
        if(this.needToSerialize(prize)) {
            node.getNode("announce").setValue(prize.isAnnounce());
            node.getNode("money").setValue(prize.getMoney());
            if(prize.getItemStacks().length > 0) {
                ArrayList<String> isList = new ArrayList<>();
                for(int i = 0; i < prize.getItemStacks().length; i++) {
                    ItemStack is = prize.getItemStacks()[i];
                    String isSer = is.getType().getName();
                    isSer += ";"+is.toContainer().getValues(true).get(DataQuery.of("UnsafeDamage"));
                    isSer += ";"+is.getQuantity();
                    if(is.get(DisplayNameData.class).isPresent())
                        isSer += ";"+is.get(DisplayNameData.class).get().displayName().get().toPlain();
                    if(is.get(LoreData.class).isPresent()) {
                        List<Text> lore = is.get(LoreData.class).get().asList();
                        StringBuilder loreOneLine = new StringBuilder();
                        for(Text text : lore) {
                            loreOneLine.append(text.toPlain());
                            //loreOneLine.append("\n");
                        }
                        isSer += ";"+loreOneLine;
                    }
                    isList.add(isSer);
                }
                node.getNode("items").setValue(isList);
            }
        }
    }

    private boolean needToSerialize(Prize prize) {
        return prize != null && (prize.getMoney() > 0 || !prize.isAnnounce() || prize.getItemStacks().length > 0);
    }

    /**
     * Syntax : [ModID]:{ItemID};[Variant];[Count];[DisplayName];[Lore]
     * Where {...} is obligatory and [...] not
     */
    public static ItemStack getStackBySyntax(String syntax) {
        String[] itemSplit = syntax.split(";");
        ItemType it = ItemTypes.NONE;
        int damage = 0;
        int count = 1;
        String variant = "";
        String customName = "";
        ArrayList<Text> lore = Lists.newArrayList();
        if ((itemSplit.length >= 1 && itemSplit[0].contains(":")) || (!syntax.contains(";") && syntax.contains(":"))) {
            String[] itemID = syntax.split(":");
            if (itemID[1].contains(";"))
                itemID[1] = itemID[1].split(";")[0];
            if (itemID.length > 2)
                console.sendMessage(TextUtils.Console.creatorError("  An item's id contains two or more \":\" (\"" + syntax + "\")"));
            else if (itemID.length < 2)
                console.sendMessage(TextUtils.Console.creatorError("  An item's id contains only the mod's id or the name's item."
                        + " Delete the \":\" or add the mod's id / name's item (\"" + syntax + "\")"));
            else {
                it = Sponge.getRegistry().getType(ItemType.class, (itemID[0] + ":" + itemID[1])).orElse(ItemTypes.NONE);
                if (!itemID[1].equals("NONE") && it.getType().equals(ItemTypes.NONE))
                    console.sendMessage(TextUtils.Console.creatorError("  The item's id (\"" + itemID[1] + "\") doesn't exist"));
            }
        } else {
            String itemID = syntax.contains(";") ? itemSplit[0] : syntax;
            it = Sponge.getRegistry().getType(ItemType.class, ("minecraft:" + itemID)).orElse(ItemTypes.NONE);
            if (!itemID.equals("NONE") && it.getType().equals(ItemTypes.NONE))
                console.sendMessage(TextUtils.Console.creatorError("  The item's id (\"" + itemID + "\") doesn't exist"));
        }
        if (itemSplit.length >= 2) {
            if (StringUtils.isNumeric(itemSplit[1])) {
                if (Integer.valueOf(itemSplit[1]) >= 0)
                    damage = Integer.valueOf(itemSplit[1]);
                else
                    console.sendMessage(TextUtils.Console.creatorError("  The items's damage is negative (\"" + syntax + "\" -> \"" + itemSplit[1] + "\")"));
            } else
                variant = itemSplit[1];
        }
        if (itemSplit.length >= 3) {
            if (StringUtils.isNumeric(itemSplit[2])) {
                if (Integer.valueOf(itemSplit[2]) >= 0)
                    count = Integer.valueOf(itemSplit[2]);
                else
                    console.sendMessage(TextUtils.Console.creatorError("  The items's count is negative (\"" + syntax + "\" -> \"" + itemSplit[2] + "\")"));
            } else
                console.sendMessage(TextUtils.Console.creatorError("  The item's count isn't an number (\"" + syntax + "\" -> \"" + itemSplit[2] + "\")"));
        }
        if (itemSplit.length >= 4) {
            if (!itemSplit[3].isEmpty())
                customName = itemSplit[3];
            else
                console.sendMessage(TextUtils.Console.creatorError("  The item's name is empty (\"" + syntax + "\" -> \"" + itemSplit[3] + "\")"));
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
                console.sendMessage(TextUtils.Console.creatorError("  The item's lore is empty (\"" + syntax + "\" -> \"" + itemSplit[4] + "\")"));
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
                                        console.sendMessage(TextUtils.Console.creatorError("  The variant \"" + variant + "\" isn't applicable for the item \""
                                                + is.getType().getId() + "\" {\"" + syntax + "\" -> \"" + itemSplit[1] + "\"}"));
                                }
                            }
                        }
                    }
                }
                if (!variantExist)
                    console.sendMessage(TextUtils.Console.creatorError("  No variant named \"" + variant + "\" has been found {\"" + syntax + "\" -> \"" + itemSplit[1] + "\")"));
            }
        } else if (damage > 0)
            is = ItemStack.builder().fromContainer(is.toContainer().set(DataQuery.of("UnsafeDamage"), damage)).build();
        return is;
    }
}
