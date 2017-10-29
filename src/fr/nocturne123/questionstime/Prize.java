package fr.nocturne123.questionstime;

import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
 
public class Prize {

	private int money;
	private boolean announce;
	private ItemStack[] items;
	
	@SuppressWarnings("unchecked")
	public Prize(Optional<CommentedConfigurationNode> prizeNode) {
		if(prizeNode.isPresent()) {
			CommentedConfigurationNode prize = prizeNode.get();
			this.money = prize.getNode("money") != null ? prize.getNode("money").getInt() : 0;
			this.announce = prize.getNode("announce") != null ? prize.getNode("announce").getBoolean() : true;
			
			CommentedConfigurationNode items = prize.getNode("items");
			if(items.getValue() != null) {
				ArrayList<ItemStack> isList = new ArrayList<>();
				items.getChildrenList().forEach(preItemNode -> {
					String preItem = preItemNode.getString();
					try {
						String[] itemSplit = preItem.split(";");
						ItemType it = ItemTypes.AIR;
						int damage = 0;
						int count = 1;
						String variant = "";
						
						if((itemSplit.length >= 1 && itemSplit[0].contains(":")) || (!preItem.contains(";") && preItem.contains(":"))) {
							String[] itemID = preItem.split(":");
							if(itemID[1].contains(";"))
								itemID[1] = itemID[1].split(";")[0];
							if(itemID.length > 2)
								QuestionsTime.getInstance().getLogger().warn("An item's id contains two or more \":\" (\""+preItem+"\")");
							else if(itemID.length < 2)
								QuestionsTime.getInstance().getLogger().warn("An item's id contains only the mod's id or the name's item."
										+ " Delete the \":\" or add the mod's id / name's item (\""+preItem+"\")");
							else {
								it = Sponge.getRegistry().getType(ItemType.class, (itemID[0]+":"+itemID[1])).orElse(ItemTypes.AIR);
								if(!itemID[1].equals("air") && it.getType().equals(ItemTypes.AIR))
									QuestionsTime.getInstance().getLogger().warn("The item's id (\""+itemID+"\") doesn't exist");
							}
						} else {
							String itemID = preItem.contains(";") ? itemSplit[0] : preItem;
							it = Sponge.getRegistry().getType(ItemType.class, ("minecraft:"+itemID)).orElse(ItemTypes.AIR);
							if(!itemID.equals("air") && it.getType().equals(ItemTypes.AIR))
								QuestionsTime.getInstance().getLogger().warn("The item's id (\""+itemID+"\") doesn't exist");
						}
						if(itemSplit.length >= 2) {
							if(StringUtils.isNumeric(itemSplit[1])) {
								if(Integer.valueOf(itemSplit[1]) >= 0)
									damage = Integer.valueOf(itemSplit[1]);
								else
									QuestionsTime.getInstance().getLogger().warn("The items's damage is negative (\""+preItem+"\" -> \""+itemSplit[1]+"\")");
							} else
								variant = itemSplit[1];
						}
						if(itemSplit.length >= 3) {
							if(StringUtils.isNumeric(itemSplit[2])) {
								if(Integer.valueOf(itemSplit[2]) >= 0)
									count = Integer.valueOf(itemSplit[2]);
								else
									QuestionsTime.getInstance().getLogger().warn("The items's count is negative (\""+preItem+"\" -> \""+itemSplit[2]+"\")");
							} else
								QuestionsTime.getInstance().getLogger().warn("The item's count isn't an number (\""+preItem+"\" -> \""+itemSplit[2]+"\")");
						}
						
						ItemStack is = ItemStack.builder().itemType(it).quantity(count).build();
						boolean variantExist = false;
						if(!variant.isEmpty()) {
							searchVariant: {
								for(@SuppressWarnings("rawtypes") Key key : Sponge.getRegistry().getAllOf(Key.class)) {
									if(key.getElementToken().isSubtypeOf(CatalogType.class)) {
										for(CatalogType element : Sponge.getRegistry().getAllOf((Class<CatalogType>) key.getElementToken().getRawType())) {
											if(!element.getName().equals("none")) {
												if(element.getName().equals(variant)) {
													variantExist = true;
													if(is.supports(key)) {
														is.offer(key, element);
														break searchVariant;
													} else
														QuestionsTime.getInstance().getLogger().info("The variant \""+variant+"\" isn't applicable for the item \""
																+is.getType().getId()+"\" {\""+preItem+"\" -> \""+itemSplit[2]+"\"}");
												}
											}
										}
									}
								}
								if(!variantExist)
									QuestionsTime.getInstance().getLogger().error("No variant named \""+variant+"\" has been found {\""+preItem+"\" -> \""+itemSplit[2]+"\")");
							}
						}
						isList.add(is);
					} catch (Exception e) {
						QuestionsTime.getInstance().getLogger().error("Error when loading an item {\""+preItem+"\"}");
						e.printStackTrace();
					}
				});
				if(!isList.isEmpty()) {
					this.items = new ItemStack[isList.size()];
					this.items = isList.toArray(this.items);
				} else
					this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.AIR).quantity(1).build()};
			} else
				this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.AIR).quantity(1).build()};
		} else {
			this.money = 0;
			this.announce = true;
			this.items = new ItemStack[] {ItemStack.builder().itemType(ItemTypes.AIR).quantity(1).build()};
		}
	}
	
	public int getMoney() {
		return money;
	}
	
	public ItemStack[] getItems() {
		return this.items;
	}
	
	public boolean isAnnounce() {
		return announce;
	}

	
}
