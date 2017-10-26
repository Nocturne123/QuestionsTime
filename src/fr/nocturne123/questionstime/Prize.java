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
	//					System.out.println("FINAL ITEM: "+it+" - "+damage+" - "+count);
	//					String[] itemSplit = preItem.split(":");
	//					if(itemSplit.length == 0)
	//						QuestionsTime.getInstance().getLogger().warn("Prize : an item is empty (ex : \"\")");
	//					else {
	//						ItemType it = ItemTypes.AIR;
	//						int damage = 0;
	//						int count = 0;
	//						if(itemSplit.length == 1)						
	//							it = Sponge.getRegistry().getAllOf(ItemType.class).stream()
	//									.filter(item2 -> item2.getId().equals("minecraft:"+itemSplit[0]))
	//									.findFirst()
	//									.orElse(ItemTypes.NONE);
	//						isList.add(ItemStack.builder().itemType(it).quantity(1).build());
	//					}
	//						
	//						ItemType it = ItemTypes.AIR;
	//						int damage = 0;
	//						int count = 1;
	//						if(itemSplit.length == 3) {
	//							it = Sponge.getRegistry().getAllOf(ItemType.class).stream()
	//								.filter(item2 -> item2.getId().equals("minecraft:"+itemSplit[0]))
	//								.findFirst()
	//								.orElse(ItemTypes.NONE);
	//							damage = StringUtils.isNumeric(itemSplit[1]) ? Integer.valueOf(itemSplit[2]) : 0;
	//							count = itemSplit.length >= 4 && StringUtils.isNumeric(itemSplit[3]) ? Integer.valueOf(itemSplit[3]) : 1;
	//						}
	//						int damage = itemSplit.length >= 3 && StringUtils.isNumeric(itemSplit[2]) ? Integer.valueOf(itemSplit[2]) : 0;
	//						int count = itemSplit.length >= 4 && StringUtils.isNumeric(itemSplit[3]) ? Integer.valueOf(itemSplit[3]) : 1;
//						DataContainer datas = is.toContainer();
//						datas.set(DataQuery.of("UnsafeDamage"), 3);
//						is.setRawData(datas);
	//					DataContainer lol = is.toContainer();
	//					lol.set(DataQuery.of("/", "UnsafeDamage"), damage);
	//					is.setRawData(lol);
						
	//					 ItemStack is2 = ItemStack.builder()
	//					            .fromContainer(is.toContainer().set(DataQuery.of("UnsafeData"), damage))
	//					            .build();
						
						isList.add(is);
					} catch (Exception e) {
						QuestionsTime.getInstance().getLogger().error("Error when loading an item {\""+preItem+"\"}");
						e.printStackTrace();
					}
				});
//				try {
//				ItemStack is = itemNode.getValue(TypeToken.of(ItemStack.class));
//				
////				ItemStackSnapshot isSnap = is.createSnapshot();
////				isSnap.
//				
//				System.out.println("ADD ITEM PRIZE :"+is);
//				isList.add(is);
//			} catch (ObjectMappingException e) {
//				e.printStackTrace();
//			}
//				System.out.println("ITEM NUMBER : "+items.getChildrenList().size());
//				int position = 1;
//				while(iter.hasNext()) {
//					CommentedConfigurationNode item = iter.next();
//					try {
//						ItemStack is = item.getNode("item"+position).getValue(TypeToken.of(ItemStack.class));
//						System.out.println("ADD ITEM PRIZE :"+is);
//						isList.add(is);
//					} catch (ObjectMappingException e) {
//						e.printStackTrace();
//					}
//				
//					String[] itemData = item.getString().toLowerCase().split("-");
//					ItemType type = Sponge.getRegistry().getAllOf(ItemType.class).stream()
//								.filter(item2 -> item2.getId().equals(itemData[0]+":"+itemData[1]))
//								.findFirst()
//								.orElse(ItemTypes.NONE);
//					int damage = itemData.length >= 3 ? StringUtils.isNumeric(itemData[2]) ? Integer.valueOf(itemData[2]) : 0 : 0;
//					int count = itemData.length >= 4 ? StringUtils.isNumeric(itemData[3]) ? Integer.valueOf(itemData[3]) : 1 : 1;
//					ItemStack is = ItemStack.builder().itemType(type).quantity(count).build();
//					is.toContainer().set(DataQuery.of("UnsafeDamage"), damage);
//					System.out.println(is.toContainer().get(DataQuery.of("UnsafeDamage")).get()+" - "+damage+" - "+is.toContainer().getValues(true));
//					Iterator<Entry<DataQuery, Object>> iter2 = is.toContainer().getValues(true).entrySet().iterator();
//					while(iter2.hasNext()) {
//						Entry<DataQuery, Object> entry = iter2.next();
//						if(entry.getKey().asString("/").equals("UnsafeDamage"))
//							entry.setValue(damage);
//					}
//					for(Entry<DataQuery, Object> entry : is.toContainer().getValues(true).entrySet()) {
//						if(entry.getKey().asString("/").equals("UnsafeDamage"))
//							entry.setValue(damage);
//					}
//				}
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
