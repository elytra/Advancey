package com.elytradev.advancey;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.elytradev.concrete.reflect.invoker.Invoker;
import com.elytradev.concrete.reflect.invoker.Invokers;
import com.google.common.base.Charsets;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.impl.SyntaxError;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.AdvancementTreeNode;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class AdvanceyAdvancementManager extends AdvancementManager {
	
	private static final Accessor<AdvancementList> acc_ADVANCEMENT_LIST = Accessors.findField(AdvancementManager.class, "field_192784_c", "ADVANCEMENT_LIST");
	private static final Accessor<Boolean> acc_hasErrored = Accessors.findField(AdvancementManager.class, "field_193768_e", "hasErrored");
	
	private static final Invoker inv_loadCustomAdvancements = Invokers.findMethod(AdvancementManager.class, "loadCustomAdvancements", "func_192781_c");
	
	public static final AdvancementList ADVANCEMENT_LIST = acc_ADVANCEMENT_LIST.get(null);
	
	private static Field field_advancementsDir;
	
	private File advancementsDir;
	private File configDir;
	
	private boolean initialized = false;

	public AdvanceyAdvancementManager(File advancementsDir, File configDir) {
		super(advancementsDir);
		this.advancementsDir = advancementsDir;
		this.configDir = configDir;
		initialized = true;
		reload();
	}
	
	@Override
	public void reload() {
		if (!initialized) return;
		// Copied from vanilla code. Reformatted, edits marked.
		acc_hasErrored.set(this, false); // ADVANCEY - Use accessor
		ADVANCEMENT_LIST.clear();
		setAdvancementsDir(convertFromJankson(configDir)); // ADVANCEY - Load config advancements first, to allow world advancements to override
		Map<ResourceLocation, Advancement.Builder> map = this.loadCustomAdvancements();
		// ADVANCEY START - Load world advancements
		setAdvancementsDir(advancementsDir);
		map.putAll(loadCustomAdvancements());
		// ADVANCEY END
		// ADVANCEY START - Don't load builtin advancements
		/*
		this.loadBuiltInAdvancements(map);
		this.hasErrored |= net.minecraftforge.common.ForgeHooks
				.loadAdvancements(map);
		*/
		// ADVANCEY END
		ADVANCEMENT_LIST.loadAdvancements(map);

		for (Advancement advancement : ADVANCEMENT_LIST.getRoots()) {
			if (advancement.getDisplay() != null) {
				AdvancementTreeNode.layout(advancement);
			}
		}
	}
	
	private File convertFromJankson(File in) {
		if (in == null) return null;
		File out = new File("tmp/advancey/"+Long.toHexString(ThreadLocalRandom.current().nextLong()));
		out.mkdirs();
		try {
			Jankson jkson = Jankson.builder().build();
			Path inRoot = in.toPath().toAbsolutePath();
			Path outRoot = out.toPath().toAbsolutePath();
			Files.walkFileTree(inRoot, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path outPath = outRoot.resolve(inRoot.relativize(dir));
					Files.createDirectories(outPath);
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path outPath = outRoot.resolve(inRoot.relativize(file));
					if (file.getFileName().toString().endsWith(".jkson") || file.getFileName().toString().endsWith(".hjson")) {
						try {
							outPath = outPath.resolveSibling(outPath.getFileName().toString().replaceFirst("\\.(jk|hj)son$", ".json"));
							JsonObject obj = jkson.load(file.toFile());
							if (!obj.containsKey("criteria")) {
								// fill in a default
								JsonObject impossible = new JsonObject();
								impossible.put("trigger", new JsonPrimitive("minecraft:impossible"));
								JsonObject criteria = new JsonObject();
								criteria.put("advancey_default", impossible);
								obj.put("criteria", criteria);
							} else if (obj.get("criteria") instanceof JsonPrimitive) {
								JsonObject trigger = new JsonObject();
								trigger.put("trigger", obj.get("criteria"));
								JsonObject criteria = new JsonObject();
								criteria.put("advancey_default", trigger);
								obj.put("criteria", criteria);
							}
							Files.write(outPath, Collections.singleton(obj.toJson(false, false)), Charsets.UTF_8);
						} catch (SyntaxError e) {
							Advancey.log.error("Syntax error in {} - {}", file, e.getCompleteMessage());
						}
					} else {
						try {
							Files.deleteIfExists(outPath);
							Files.createLink(outPath, file);
						} catch (Exception e) {
							// if we can't hardlink, fall back to a copy
							Files.copy(file, outPath, StandardCopyOption.REPLACE_EXISTING);
						}
					}
					return FileVisitResult.CONTINUE;
				}
				
				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
					Advancey.log.error("Error while visiting {} for transform", file, e);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			Advancey.log.error("Error while transforming {}", in, e);
		}
		return out;
	}
	
	private void setAdvancementsDir(File f) {
		Advancey.log.info("Loading advancements from {}", f);
		// no, officer, I'm not setting a final field....
		try {
			if (field_advancementsDir == null) {
				field_advancementsDir = ReflectionHelper.findField(AdvancementManager.class, "field_192785_d", "advancementsDir");
			}
			EnumHelper.setFailsafeFieldValue(field_advancementsDir, this, f);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private Map<ResourceLocation, Advancement.Builder> loadCustomAdvancements() {
		return (Map<ResourceLocation, Advancement.Builder>)inv_loadCustomAdvancements.invoke(this);
	}
	
}
