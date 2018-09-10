package com.elytradev.advancey;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends Proxy {

	private final Minecraft mc = Minecraft.getMinecraft();
	
	@Override
	public void preInit() {
		
	}

}
