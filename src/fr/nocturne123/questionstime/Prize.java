package fr.nocturne123.questionstime;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import com.google.common.collect.Lists;

import fr.nocturne123.questionstime.util.TextUtils;
import ninja.leaping.configurate.ConfigurationNode;
 
public class Prize {

	private int money;
	private boolean announce;
	private ItemStack[] items;
	
	@SuppressWarnings("unchecked")
	public Prize(Optional<ConfigurationNode> prizeNode) {
		if(prizeNode != null && prizeNode.isPresent()) {
			ConfigurationNode prize = prizeNode.get();
			this.money = prize.getNode("money") != null ? prize.getNode("money").getInt() : 0;
			this.announce = prize.getNode("announce") != null ? prize.getNode("announce").getBoolean() : true;
			ConsoleSource console = QuestionsTime.getInstance().getConsole();
			
			ConfigurationNode items = prize.getNode("items");
			if(items.getValue() != null) {
				ArrayList<ItemStack> isList = new ArrayList<>();
				items.getChildrenList().forEach(preItemNode -> {
					String preItem = preItemNode.getString();
					try {
						String[] itemSplit = preItem.split(";");
						ItemType it = ItemTypes.NONE;
						int damage = 0;
						int count = 1;
						String variant = "";
						String customName = "";
						ArrayList<Text> lore = Lists.newArrayList();
						
						if((itemSplit.length >= 1 && itemSplit[0].contains(":")) || (!preItem.contains(";") && preItem.contains(":"))) {
							String[] itemID = preItem.split(":");
							if(itemID[1].contains(";"))
								itemID[1] = itemID[1].split(";")[0];
							if(itemID.length > 2)
								console.sendMessage(TextUtils.Console.creatorError("  An item's id contains two or more \":\" (\""+preItem+"\")"));
							else if(itemID.length < 2)
								console.sendMessage(TextUtils.Console.creatorError("  An item's id contains only the mod's id or the name's item."
										+ " Delete the \":\" or add the mod's id / name's item (\""+preItem+"\")"));
							else {
								it = Sponge.getRegistry().getType(ItemType.class, (itemID[0]+":"+itemID[1])).orElse(ItemTypes.NONE);
								if(!itemID[1].equals("NONE") && it.getType().equals(ItemTypes.NONE))
									console.sendMessage(TextUtils.Console.creatorError("  The item's id (\""+itemID[1]+"\") doesn't exist"));
							}
						} else {
							String itemID = preItem.contains(";") ? itemSplit[0] : preItem;
							it = Sponge.getRegistry().getType(ItemType.class, ("minecraft:"+itemID)).orElse(ItemTypes.NONE);
							if(!itemID.equals("NONE") && it.getType().equals(ItemTypes.NONE))
								console.sendMessage(TextUtils.Console.creatorError("  The item's id (\""+itemID+"\") doesn't exist"));
						}
						if(itemSplit.length >= 2) {
							if(StringUtils.isNumeric(itemSplit[1])) {
								if(Integer.valueOf(itemSplit[1]) >= 0)
									damage = Integer.valueOf(itemSplit[1]);
								else
									console.sendMessage(TextUtils.Console.creatorError("  The items's damage is negative (\""+preItem+"\" -> \""+itemSplit[1]+"\")"));
							} else
								variant = itemSplit[1];
						}
						if(itemSplit.length >= 3) {
							if(StringUtils.isNumeric(itemSplit[2])) {
								if(Integer.valueOf(itemSplit[2]) >= 0)
									count = Integer.valueOf(itemSplit[2]);
								else
									console.sendMessage(TextUtils.Console.creatorError("  The items's count is negative (\""+preItem+"\" -> \""+itemSplit[2]+"\")"));
							} else
								console.sendMessage(TextUtils.Console.creatorError("  The item's count isn't an number (\""+preItem+"\" -> \""+itemSplit[2]+"\")"));
						}
						if(itemSplit.length >= 4) {
							if(!itemSplit[3].isEmpty())
								customName = itemSplit[3];
							else
								console.sendMessage(TextUtils.Console.creatorError("  The item's name is empty (\""+preItem+"\" -> \""+itemSplit[3]+"\")"));
						}
						if(itemSplit.length >= 5) {
							if(!itemSplit[4].isEmpty()) {
								String loreOneLine = itemSplit[4];
								if(loreOneLine.contains("\n")) {
									for(String line : loreOneLine.split("\n"))
										lore.add(Text.of(line));
								} else
									lore.add(Text.of(loreOneLine));
							}
							else
								console.sendMessage(TextUtils.Console.creatorError("  The item's lore is empty (\""+preItem+"\" -> \""+itemSplit[4]+"\")"));
						}
						
						ItemStack is = ItemStack.builder()
								.itemType(it)
								.quantity(count)
								.add(Keys.DISPLAY_NAME, Text.of(customName))
								.add(Keys.ITEM_LORE, lore).build();
						
						boolean variantExist = false;
						if(!variant.isEmpty() && QuestionsTime.getInstance().getSpongeAPI() == 7) {
							searchVariant: {
							for(@SuppressWarnings("rawtypes") Key key : Sponge.getRegistry().getAllOf(Key.class)) {
								if(CatalogType.class.isAssignableFrom(key.getElementToken().getRawType())) {
									for(CatalogType element : Sponge.getRegistry().getAllOf((Class<CatalogType>) key.getElementToken().getRawType())) {
										
										String elmtID = element.getId();
										if(elmtID.contains(":")) {
											if(elmtID.split(":").length >= 2 && !elmtID.split(":")[1].isEmpty())
												elmtID = elmtID.split(":")[1];
										}

										if(!elmtID.equals("none")) {
											if(elmtID.equals(variant)) {
												variantExist = true;
												if(is.supports(key)) {
													is.offer(key, element);
													break searchVariant;
												} else
													console.sendMessage(TextUtils.Console.creatorError("  The variant \""+variant+"\" isn't applicable for the item \""
															+is.getType().getId()+"\" {\""+preItem+"\" -> \""+itemSplit[1]+"\"}"));
											}
										}
									}
								}
							}
							if(!variantExist)
								console.sendMessage(TextUtils.Console.creatorError("  No variant named \""+variant+"\" has been found {\""+preItem+"\" -> \""+itemSplit[1]+"\")"));
							}
						} else if(damage > 0)
							is = ItemStack.builder().fromContainer(is.toContainer().set(DataQuery.of("UnsafeDamage"), damage)).build();
						isList.add(is);
					} catch (Exception e) {
						console.sendMessage(TextUtils.Console.creatorError("  Error when loading an item {\""+preItem+"\"}"));
						e.printStackTrace();
					}
				});
				if(!isList.isEmpty()) {
					this.items = new ItemStack[isList.size()];
					this.items = isList.toArray(this.items);
				} else
					this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.NONE).quantity(1).build()};
			} else
				this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.NONE).quantity(1).build()};
		} else {
			this.money = 0;
			this.announce = true;
			this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.NONE).quantity(1).build()};
		}
	}
	
	public Prize(int money, boolean announce, ItemStack[] is) {
		this.money = money > 0 ? money : 0;
		this.announce = announce;
		if(is != null)
			this.items = is;
		else
			this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.NONE).quantity(1).build()};
	}
	
	public int getMoney() {
		return money;
	}
	
	public ItemStack[] getTypes() {
		return this.items;
	}
	
	public boolean isAnnounce() {
		return announce;
	}

	
}
