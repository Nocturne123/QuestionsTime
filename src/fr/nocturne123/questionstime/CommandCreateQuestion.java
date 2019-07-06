package fr.nocturne123.questionstime;

import fr.nocturne123.questionstime.util.TextUtils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

public class CommandCreateQuestion implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if(src instanceof Player) {
			if(!QuestionsTime.getInstance().isCreator(((Player) src).getUniqueId())) {
				Text qtPre = QuestionsTime.getInstance().qtPrefix;
				src.sendMessage(Text.join(qtPre, Text.builder("--------Question Creator--------").color(TextColors.DARK_GREEN).style(TextStyles.BOLD).build()));
				src.sendMessage(Text.join(qtPre, Text.builder(" A few things before starting :").color(TextColors.GREEN).build()));
				src.sendMessage(Text.join(qtPre, Text.builder(" • You need to start your message by \"").color(TextColors.GREEN).build(), 
						Text.builder("qtc>answer").color(TextColors.BLUE).build(), 
						Text.builder("\" everytimes").color(TextColors.GREEN).build()));
				src.sendMessage(Text.join(qtPre, Text.builder(" • Each times, you need to confirm with \"").color(TextColors.GREEN).build(),
						Text.builder("qtc>confirm").color(TextColors.BLUE).build(), 
						Text.builder("\". I know that can be annoying but it's for avoid error(s).").color(TextColors.GREEN).build()));
				src.sendMessage(Text.join(qtPre, Text.builder(" • You can stop whenever by typing : \"").color(TextColors.GREEN).build(),
						Text.builder("qtc>stop").color(TextColors.BLUE).build(),
						Text.builder("\"").color(TextColors.GREEN).build()));
				src.sendMessage(Text.join(qtPre, Text.builder(" • Everything you will type will be personnal and not send in the chat").color(TextColors.GREEN).build()));
				src.sendMessage(TextUtils.creatorNormalWithPrefix(" • If you quit the server while you were creating a question, you lose your progress"));
				src.sendMessage(Text.join(qtPre, Text.builder(" When your ready, type : \"").color(TextColors.GREEN).build(),
						Text.builder("qtc>start").color(TextColors.BLUE).build(),
						Text.builder("\"").color(TextColors.GREEN).build()));
			} else
				src.sendMessage(TextUtils.creatorNormalWithPrefix("You're already creating a question !"));
		} else
			src.sendMessage(Text.of("The command can only be done by a player. Sorry :("));
		return CommandResult.success();
	}

}
