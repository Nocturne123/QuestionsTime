package fr.nocturne123.questionstime;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.ExplosionRadiusData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

public class GadgetThunder {
	
	@Listener
	public void onPlayerClickRight(InteractBlockEvent.Secondary e){
		e.getCause().first(Player.class)
			.ifPresent(p -> p.getItemInHand(HandTypes.MAIN_HAND)
				.filter(predicate -> predicate.getType().equals(ItemTypes.BLAZE_ROD))
				.ifPresent(is -> e.getTargetBlock().getLocation()
						.ifPresent(loc -> this.spawnModifiedEntity(loc.add(0.5, 1, 0.5), EntityTypes.CREEPER))));
		
//		if(e.getCause().first(Player.class).isPresent()){
//			Player p = e.getCause().first(Player.class).get();
//			if(p.getItemInHand(HandTypes.MAIN_HAND).isPresent() && p.getItemInHand(HandTypes.MAIN_HAND).get().getItem().equals(ItemTypes.BLAZE_ROD)){
//				Optional<Location<World>> loc = e.getTargetBlock().getLocation();
//				if(loc.isPresent()){
//					Location<World> blockPos = loc.get().add(0.5, 1, 0.5);
//					this.spawnModifiedEntity(blockPos, EntityTypes.CREEPER);
//				}
//			}
//		}
	}
	
	public void spawnModifiedEntity(Location<World> spawnLocation, EntityType et) {
	    Extent extent = spawnLocation.getExtent();
	    Entity ent = extent.createEntity(et, spawnLocation.getPosition());
	    extent.spawnEntity(ent);
	    	if(ent.getType().equals(EntityTypes.CREEPER)){
	    		ExplosionRadiusData radiusData = ent.get(ExplosionRadiusData.class).get();
	        	ent.offer(radiusData.explosionRadius().setTo(50));
	        	ent.offer(Keys.DISPLAY_NAME, Text.of(TextColors.DARK_RED, "CREEPER BOOSTED"));
	        	ent.offer(Keys.AI_ENABLED, false);
	        }
	}

}
