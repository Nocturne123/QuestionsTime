package fr.nocturne123.questionstime.util;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import fr.nocturne123.questionstime.QuestionsTime;

public class TextUtils {

	private static QuestionsTime instance = QuestionsTime.getInstance();
	
	public static Text creatorNormal(String message) {
		return Text.builder(message).color(TextColors.GREEN).build();
	}
	
	public static Text creatorNormalWithPrefix(String message) {
		return Text.join(instance.qtPrefix, creatorNormal(" "+message));
	}
	
	public static Text creatorSpecial(String message) {
		return Text.builder(message).color(TextColors.BLUE).build();
	}
	
	public static Text creatorSpecialWithPrefix(String message) {
		return Text.join(instance.qtPrefix, creatorSpecial(" "+message));
	}
	
	public static Text creatorComposed(String normalOne, String special, String normalTwo) {
		return Text.join(instance.qtPrefix, creatorNormal(" "+normalOne), creatorSpecial(special), creatorNormal(normalTwo));
	}
	
	public static Text creatorComposedWhitoutPrefix(String normalOne, String special, String normalTwo) {
		return Text.join(creatorNormal(normalOne), creatorSpecial(special), creatorNormal(normalTwo));
	}
	
	public static void sendTextToEveryone(Text text) {
		Sponge.getServer().getOnlinePlayers().forEach(player -> {
			player.sendMessage(Text.join(instance.qtPrefix, text));
		});
	}
	
	public static Text readableItemID(ItemStack is) {
		String itemID = is.getItem().getId();
		if(itemID.isEmpty())
			return Text.of("itemIDEmpty");
		String finalID = "";
		Text textModID = Text.builder("").build();
		if(itemID.startsWith("minecraft:")) {
			finalID = itemID.substring(10);
			finalID = finalID.substring(0, 1).toUpperCase()+finalID.substring(1, finalID.length());
		} else if(itemID.contains(":") && !itemID.split(":")[0].isEmpty()) {
			String modID = itemID.split(":")[0];
			Optional<PluginContainer> pluginCont = Sponge.getPluginManager().getPlugin(modID);
			if(pluginCont.isPresent()) {
				String modName = pluginCont.get().getName();
				String itemName = itemID.split(":")[1];
				textModID = Text.builder(modName.substring(0, 1).toUpperCase()+modName.substring(1, modName.length())+": ").color(TextColors.BLUE).build();
				finalID = itemName.substring(0, 1).toUpperCase()+itemName.substring(1, itemName.length());
			} else {
				instance.getLogger().error("No mod with ID \""+modID+"\" was found.");
				finalID = modID+" - "+itemID+" -> No found";
			}
		} else
			finalID = "error{"+itemID+"}";
		if(finalID.contains("_"))
			finalID = finalID.replace('_', ' ');
		Map<DataQuery, Object> keys = is.toContainer().getValues(true);
		Text metadataText = Text.builder().build();
		if(keys.containsKey(DataQuery.of("UnsafeDamage")) && keys.get(DataQuery.of("UnsafeDamage")) instanceof Integer)
			if(((int) keys.get(DataQuery.of("UnsafeDamage"))) != 0)
				metadataText = Text.builder(" "+keys.get(DataQuery.of("UnsafeDamage"))).color(TextColors.AQUA).build();
		return Text.join(textModID, Text.builder(finalID).color(TextColors.WHITE).build(), metadataText);
	}

	public static class Console {
		
		public static Text creatorNormal(String message) {
			return Text.builder(message).color(TextColors.YELLOW).build();
		}
		
		public static Text creatorNormalWithPrefix(String message) {
			return Text.join(instance.qtPrefix, creatorNormal(" "+message));
		}
		
		public static Text creatorSpecial(String message) {
			return Text.builder(message).color(TextColors.BLUE).build();
		}
		
		public static Text creatorSpecialWithPrefix(String message) {
			return Text.join(instance.qtPrefix, creatorSpecial(" "+message));
		}
		
		public static Text creatorComposed(String normalOne, String special, String normalTwo) {
			return Text.join(instance.qtPrefix, creatorNormal(" "+normalOne), creatorSpecial(special), creatorNormal(normalTwo));
		}
		
		public static Text creatorComposedWhitoutPrefix(String normalOne, String special, String normalTwo) {
			return Text.join(creatorNormal(normalOne), creatorSpecial(special), creatorNormal(normalTwo));
		}
		
		public static Text creatorError(String message) {
			return Text.join(instance.qtPrefix, Text.builder(message).color(TextColors.RED).build());
		}
		
	}
	
}
