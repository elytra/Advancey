package com.elytradev.advancey.trigger;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatTrigger implements ICriterionTrigger<ChatTrigger.Instance> {

	public static final ResourceLocation ID = new ResourceLocation("advancey", "chat_message_sent");
	
	private Set<ListenerEntry> listeners = Sets.newHashSet();
	
	public ChatTrigger() {
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	public void addListener(PlayerAdvancements playerAdvancementsIn, Listener<Instance> listener) {
		listeners.add(new ListenerEntry(playerAdvancementsIn, listener));
	}

	@Override
	public void removeListener(PlayerAdvancements playerAdvancementsIn, Listener<Instance> listener) {
		listeners.remove(new ListenerEntry(playerAdvancementsIn, listener));
	}

	@Override
	public void removeAllListeners(PlayerAdvancements playerAdvancementsIn) {
		listeners.clear();
	}

	@Override
	public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
		if (!json.has("phrase") || !json.get("phrase").isJsonPrimitive() || !json.get("phrase").getAsJsonPrimitive().isString())
			throw new JsonSyntaxException("Phrase must be specified");
		String phrase = json.get("phrase").getAsString().toLowerCase();
		boolean suppress = json.has("suppress") && json.get("suppress").getAsBoolean();
		return new Instance(phrase, suppress);
	}
	
	@SubscribeEvent
	public void onChat(ServerChatEvent e) {
		Iterator<ListenerEntry> iter = listeners.iterator();
		while (iter.hasNext()) {
			ListenerEntry pair = iter.next();
			if (e.getMessage().toLowerCase().contains(pair.listener.getCriterionInstance().phrase)) {
				if (pair.listener.getCriterionInstance().suppress) {
					e.setCanceled(true);
				}
				pair.listener.grantCriterion(pair.advancements);
				break;
			}
		}
	}
	
	private static class ListenerEntry {
		public final PlayerAdvancements advancements;
		public final Listener<Instance> listener;
		
		public ListenerEntry(PlayerAdvancements advancements, Listener<Instance> listener) {
			this.advancements = advancements;
			this.listener = listener;
		}
	}
	
	public static class Instance extends AbstractCriterionInstance {
		public final String phrase;
		public final boolean suppress;
		
		public Instance(String phrase, boolean suppress) {
			super(ID);
			this.phrase = phrase;
			this.suppress = suppress;
		}
	}
	
}
