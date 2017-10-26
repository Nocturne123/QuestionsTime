package fr.nocturne123.questionstime;

import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.InventoryArchetypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.property.SlotPos;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;

public class Inventory {

	@Listener
	public void onPlayerClickRight(InteractEvent e){
		if(e.getCause().first(Player.class).isPresent()){
			Player p = e.getCause().first(Player.class).get();
			p.sendMessage(Text.of("Test1"));
			if(p.getItemInHand(HandTypes.MAIN_HAND).isPresent() && p.getItemInHand(HandTypes.MAIN_HAND).get().getType().equals(ItemTypes.COMPASS)){
				this.hubInventory(p);
				p.sendMessage(Text.of("Test2"));
			}
		}
	}
	
	public void hubInventory(Player p){
		ItemStack compass = ItemStack.builder()
				.itemType(ItemTypes.COMPASS)
				.quantity(1)
				.build();
		if(p.getInventory().contains(ItemTypes.SAND)){
			ItemStack hotbar = p.getInventory().query(Hotbar.class).query(ItemTypes.SAND).peek().get();
			QuestionsTime.getInstance().getLogger().debug("LOL "+hotbar.getType());
			org.spongepowered.api.item.inventory.Inventory inv = org.spongepowered.api.item.inventory.Inventory.builder()
					.of(InventoryArchetypes.CHEST)
					.property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of("HUB")))
					.listener(ClickInventoryEvent.class, e -> QuestionsTime.getInstance().getLogger().info("WOWOWO"))
					.build(QuestionsTime.getInstance());
			
			inv.query(GridInventory.class)
				.query(SlotPos.of(1, 1))
				.offer(ItemStack.of(ItemTypes.DIAMOND, 1));
			
			p.openInventory(inv);
		}
		
//		builder.name(new FixedTranslation("HUB")).size(9);
//		CustomInventory opInv = builder.build();
//		CustomInventory ci = CustomInventory.builder()
//				.size(9)
//				.name(new FixedTranslation("HUB"))
//				.build();
//		p.openInventory(ci, Cause.of(NamedCause.simulated(GameProfile.class)));
//		p.sendMessage(Text.of("Test5"));
//		ci.set(compass);
//		p.sendMessage(Text.of("Test6"));
//		p.setItemInHand(compass);
	}
	
}
