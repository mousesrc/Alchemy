package index.alchemy.command;

import com.google.common.base.Joiner;

import index.alchemy.core.AlchemyEventSystem;
import index.project.version.annotation.Alpha;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.Side;

@Alpha
public class CommandCRun extends AlchemyCommandClient {

	@Override
	public String getName() {
		return "crun";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/crun <java-code>";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length > 0)
			AlchemyEventSystem.addDelayedRunnable(Side.CLIENT, p -> Javarepl.evaluate(sender, Joiner.on(' ').join(args)), 0);
	}

}
