package com.elytradev.advancey;

import com.elytradev.advancey.trigger.ChatTrigger;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.ICriterionTrigger;

public class AdvanceyTriggers {

	public static final ICriterionTrigger<?> CHAT_MESSAGE = new ChatTrigger();
	
	public static void register() {
		CriteriaTriggers.register(CHAT_MESSAGE);
	}
	
}
