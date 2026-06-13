package com.seibel.distanthorizons.common.commands;
#if MC_VER <= MC_1_12_2
import com.seibel.distanthorizons.core.config.ConfigHandler;
import com.seibel.distanthorizons.core.config.types.AbstractConfigBase;
import com.seibel.distanthorizons.core.config.types.ConfigEntry;
import com.seibel.distanthorizons.core.logging.f3.F3Screen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
#else
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import static net.minecraft.commands.Commands.literal;
#endif
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.seibel.distanthorizons.core.network.messages.MessageRegistry.DEBUG_CODEC_CRASH_MESSAGE;

#if MC_VER <= MC_1_21_10
#else
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
#endif

public class CommandInitializer
{
	private boolean serverReady = false;
	
	#if MC_VER <= MC_1_21_10
	private static final int REQUIRED_PERMISSION_LEVEL = 4;
	#else
	private static final PermissionCheck COMMAND_PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_OWNER);
	#endif
	
	
	
	#if MC_VER <= MC_1_12_2
	public static ICommand initCommands()
	{
		return new CommandBase()
		{
			#if MC_VER <= MC_1_7_10
			@Override
			public String getCommandName() { return "dh"; }
			
			@Override
			public String getCommandUsage(ICommandSender sender) { return "/dh <debug|config|pregen>"; }
			
			@Override
			public void processCommand(ICommandSender sender, String[] args)
			{
				this.executeCommand(MinecraftServer.getServer(), sender, args);
			}
			#else
			@Override
			public String getName() { return "dh"; }
			
			@Override
			public String getUsage(ICommandSender sender) { return "/dh <debug|config|pregen>"; }
			
			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args)
			{
				this.executeCommand(server, sender, args);
			}
			#endif
			
			private void executeCommand(MinecraftServer server, ICommandSender sender, String[] args)
			{
				if (args.length == 0)
				{
					if (DEBUG_CODEC_CRASH_MESSAGE)
					{
						AbstractCommand.sendMessage(sender, "Usage: /dh <debug|config|crash|pregen>");
					}
					else
					{
						AbstractCommand.sendMessage(sender, "Usage: /dh <debug|config|pregen>");
					}
					return;
				}
				
				switch (args[0])
				{
					case "debug":
						DebugCommand debugCommand = new DebugCommand();
						debugCommand.execute(sender);
						break;
					case "config":
						ConfigCommand configCommand = new ConfigCommand();
						configCommand.execute(sender, args);
						break;
					case "crash":
						if (DEBUG_CODEC_CRASH_MESSAGE)
						{
							CrashCommand crashCommand = new CrashCommand();
							crashCommand.execute(sender, args);
						}
						break;
					case "pregen":
						if (!server.isDedicatedServer())
						{
							AbstractCommand.sendMessage(sender, "Pregen command is only available on dedicated servers");
							break;
						}
						PregenCommand pregenCommand = new PregenCommand();
						pregenCommand.execute(server, sender, args);
						break;
					default:
						AbstractCommand.sendMessage(sender, "Unknown subcommand: " + args[0]);
				}
			}
		};
		
	}
	
	#else
	
	/**
	 * A received command dispatcher, which is held until the server is ready to initialize the commands.
	 */
	@Nullable
	private CommandDispatcher<CommandSourceStack> commandDispatcher;
	
	/**
	 * Notify the command initializer that the game is ready to accept commands.
	 * If {@link CommandInitializer#initCommands(CommandDispatcher)} has been fired before it was ready, it will also initialize the commands.
	 */
	public void onServerReady()
	{
		this.serverReady = true;
		if (this.commandDispatcher != null)
		{
			this.initCommands(this.commandDispatcher);
			this.commandDispatcher = null;
		}
	}
	
	/**
	 * Initializes all available commands.
	 * If the game is not ready yet, it stores the dispatcher to initialize the commands later.
	 *
	 * @param commandDispatcher The command dispatcher to register commands to.
	 */
	public void initCommands(CommandDispatcher<CommandSourceStack> commandDispatcher)
	{
		if (!this.serverReady)
		{
			this.commandDispatcher = commandDispatcher;
			return;
		}
		
		LiteralArgumentBuilder<CommandSourceStack> builder = literal("dh")
				.requires((source) ->
				{
					#if MC_VER <= MC_1_21_10
					return source.hasPermission(REQUIRED_PERMISSION_LEVEL);
					#else
					return COMMAND_PERMISSION_CHECK.check(source.permissions());
					#endif
				});
		
		builder.then(new ConfigCommand().buildCommand());
		builder.then(new DebugCommand().buildCommand());
		builder.then(new PregenCommand().buildCommand());
		
		if (DEBUG_CODEC_CRASH_MESSAGE)
		{
			builder.then(new CrashCommand().buildCommand());
		}
		
		commandDispatcher.register(builder);
	}
	#endif
}
