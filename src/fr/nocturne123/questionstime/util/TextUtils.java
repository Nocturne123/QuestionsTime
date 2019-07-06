package fr.nocturne123.questionstime.util;

import fr.nocturne123.questionstime.QuestionsTime;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.Map;
import java.util.Optional;

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
		String itemID = is.getType().getId();
		if(itemID.isEmpty())
			return Text.of("itemIDEmpty");
		Text finalID = Text.EMPTY;
		Text textModID = Text.builder("").build();
		if(is.get(Keys.DISPLAY_NAME).isPresent()) {
			Text displayName = is.get(Keys.DISPLAY_NAME).get();
			if(!displayName.isEmpty())
				finalID = Text.of(displayName);
		}
		if(finalID.isEmpty()) {
			if (itemID.startsWith("minecraft:"))
				finalID = Text.of(StringUtils.capitalize(itemID.substring(10)));
			else if (itemID.contains(":") && !itemID.split(":")[0].isEmpty()) {
				String modID = itemID.split(":")[0];
				Optional<PluginContainer> pluginCont = Sponge.getPluginManager().getPlugin(modID);
				if (pluginCont.isPresent()) {
					String modName = pluginCont.get().getName();
					String itemName = itemID.split(":")[1];
					textModID = Text.builder(modName.substring(0, 1).toUpperCase() + modName.substring(1) + ": ").color(TextColors.BLUE).build();
					finalID = Text.of(StringUtils.capitalize(itemName));
				} else {
					instance.getLogger().error("No mod with ID \"" + modID + "\" was found.");
					finalID = Text.of(modID + " - " + itemID + " -> No found");
				}
			} else
				finalID = Text.of("error{" + itemID + "}");
		}
		if(finalID.toPlain().contains("_"))
			finalID = Text.of(finalID.toPlain().replace('_', ' '));
		Map<DataQuery, Object> keys = is.toContainer().getValues(true);
		Text metadataText = Text.builder().build();
		if(keys.containsKey(DataQuery.of("UnsafeDamage")) && keys.get(DataQuery.of("UnsafeDamage")) instanceof Integer)
			if(((int) keys.get(DataQuery.of("UnsafeDamage"))) != 0)
				metadataText = Text.builder(" "+keys.get(DataQuery.of("UnsafeDamage"))).color(TextColors.AQUA).build();
		return Text.join(textModID, Text.builder().append(finalID).color(TextColors.WHITE).build(), metadataText);
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
