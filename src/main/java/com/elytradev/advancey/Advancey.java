package com.elytradev.advancey;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

import net.minecraft.advancements.AdvancementManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

@Mod(modid=Advancey.MODID, name=Advancey.NAME, version=Advancey.VERSION)
public class Advancey {
	public static final Logger log = LogManager.getLogger("Advancey");
	
	public static final String MODID = "advancey";
	public static final String NAME = "Advancey";
	public static final String VERSION = "@VERSION@";
	
	@SidedProxy(clientSide="com.elytradev.advancey.ClientProxy", serverSide="com.elytradev.advancey.Proxy")
	public static Proxy proxy;
	
	private static final Accessor<AdvancementManager> acc_advancementManager = Accessors.findField(World.class, "field_191951_C", "advancementManager");
	private static final Accessor<File> acc_advancementsDir = Accessors.findField(AdvancementManager.class, "field_192785_d", "advancementsDir");
	
	private int timesAdvancementManagerReplaced = 0;
	private int nextWarning = 100;
	
	private File globalAdvancementsDir;
	
	@EventHandler
	public void onPreInit(FMLPreInitializationEvent e) {
		globalAdvancementsDir = new File(e.getModConfigurationDirectory(), "advancey/advancements");
		MinecraftForge.EVENT_BUS.register(this);
		AdvanceyTriggers.register();
	}
	
	@EventHandler
	public void onServerStarting(FMLServerStartingEvent e) {
		timesAdvancementManagerReplaced = 0;
		nextWarning = 100;
		Files.fileTreeTraverser().postOrderTraversal(new File("tmp/advancey")).forEach(File::delete);
		e.registerServerCommand(new CommandBase() {
			
			@Override
			public String getUsage(ICommandSender sender) {
				return "/advancey_reload";
			}
			
			@Override
			public String getName() {
				return "advancey_reload";
			}
			
			@Override
			public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
				for (WorldServer ws : server.worlds) {
					ws.getAdvancementManager().reload();
				}
				for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
					player.getAdvancements().reload();
					player.getAdvancements().flushDirty(player);
				}
				notifyCommandListener(sender, this, "commands.advancey.reload.success", Iterables.size(AdvanceyAdvancementManager.ADVANCEMENT_LIST.getAdvancements()));
			}
		});
		replaceAdvancementManagers(e.getServer());
	}
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent e) {
		if (e.phase == Phase.START) {
			replaceAdvancementManagers(FMLCommonHandler.instance().getMinecraftServerInstance());
		}
	}

	private void replaceAdvancementManagers(MinecraftServer server) {
		for (WorldServer ws : server.worlds) {
			AdvancementManager cur = ws.getAdvancementManager();
			if (!(cur instanceof AdvanceyAdvancementManager)) {
				acc_advancementManager.set(ws, new AdvanceyAdvancementManager(acc_advancementsDir.get(cur), globalAdvancementsDir));
				timesAdvancementManagerReplaced++;
			}
		}
		if (timesAdvancementManagerReplaced > nextWarning) {
			log.warn("Advancey has had to replace the AdvancementManager over {} times. Do you have another advancement-changing mod installed?", nextWarning);
			nextWarning *= 10;
		}
	}

}
