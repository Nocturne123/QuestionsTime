package fr.nocturne123.questionstime;

import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class Command implements CommandExecutor{

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if(src instanceof Player){
				Player p = (Player) src;
				int X = (int) p.getLocation().getX();
				int Y = (int) p.getLocation().getY();
				int Z = (int) p.getLocation().getZ();
				int ping = p.getConnection().getLatency();
				p.sendMessage(Text.of("Tu te trouves en ",
							TextColors.AQUA,
							X, " ",
							Y, " ",
							Z, TextColors.WHITE,
							" et tu as ", TextColors.GREEN,
							ping, TextColors.WHITE,
							" de Ping"));
				
				//Location<World> loc = new Location<World>(p.getWorld(), X, Y, Z);
				//GadgetThunder.spawnCreeper(loc);
				//GadgetThunder.spawnEntity2(p);
		} else if(src instanceof ConsoleSource){
			src.sendMessage(Text.of("Command non utilisable depuis la console !"));
		} else src.sendMessage(Text.of("Command utilisable seulement depuis un joueur !"));
		
		//if()
		return CommandResult.success();
	}

}
