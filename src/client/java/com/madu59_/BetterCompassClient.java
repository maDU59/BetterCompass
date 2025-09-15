package com.maDU59_;


import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.lang.Math;
import java.io.*;
import java.nio.file.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.maDU59_.config.SettingsManager;
import com.maDU59_.mixin.client.FovMultiplierAccessor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;


public class BetterCompassClient implements ClientModInitializer {

	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private static final float GUI_WIDTH = 0.5F;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, Object> valueMap = new LinkedHashMap<>();


	private static RegistryKey<World> lastDimension = null;

	public static BlockPos deathPointBlockPos = null;
	public static RegistryKey<World> deathDimension = null;
	public static BlockPos netherPortalBlockPos = null;
	public static String serverId;

	@Override
	public void onInitializeClient() {
		// Attach our rendering code to before the chat hud layer. Our layer will render right before the chat. The API will take care of z spacing.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(BetterCompass.MOD_ID, "before_chat"), BetterCompassClient::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.isDead()) {
				deathPointBlockPos = client.player.getBlockPos();
				deathDimension = client.world.getRegistryKey();
				valueMap.put("deathPointBlockPos", deathPointBlockPos);
				valueMap.put("deathDimension", deathDimension);
				saveValues();
			}
			if (client.world != null) {
			RegistryKey<World> current = client.world.getRegistryKey();
				if (lastDimension != null && !lastDimension.equals(current)) {
					if (current == World.NETHER) {
						netherPortalBlockPos = client.player.getBlockPos();
						valueMap.put("netherPortalBlockPos", netherPortalBlockPos);
						saveValues();
					}
					else{
						netherPortalBlockPos = null;
					}
				}
				lastDimension = current;
			}
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// This runs when the client enters a world

			if (CLIENT.getServer() == null) {
				// Multiplayer
				ServerInfo info = CLIENT.getCurrentServerEntry();
				serverId = info != null ? info.address.replace(":", "_") : "unknown_server";
			} else {
				// Singleplayer
				serverId = CLIENT.getServer().getSavePath(WorldSavePath.ROOT)
					.getParent().getFileName().toString();
			}
			loadData();
		});
	}

	private static void render(DrawContext context, RenderTickCounter tickCounter) {
		PlayerEntity player = CLIENT.player;
		int optionValueIndex = SettingsManager.SHOW_COMPASS_HUD.getValueAsIndex();
		if(optionValueIndex == 3 
			|| (optionValueIndex == 2 && !player.getMainHandStack().getItem().getTranslationKey().equals("item.minecraft.compass"))
			|| (optionValueIndex == 1 && !player.getInventory().containsAny(Set.of(Items.COMPASS)))
		){
			return;
		}
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		Camera camera = CLIENT.gameRenderer.getCamera();
        if (camera == null) return;

		float fov = ((FovMultiplierAccessor)(Object)CLIENT.gameRenderer).getFovMultiplier() * CLIENT.options.getFov().getValue();
		float camDirection = camera.getYaw();

		drawCompassSymbol(context, textRenderer, fov, "N", 180, camDirection, 0.05F, 0xFFFFFFFF);
		drawCompassSymbol(context, textRenderer, fov, "E", 270, camDirection, 0.05F, 0xFFFFFFFF);
		drawCompassSymbol(context, textRenderer, fov, "S", 0, camDirection, 0.05F, 0xFFFFFFFF);
		drawCompassSymbol(context, textRenderer, fov, "W", 90, camDirection, 0.05F, 0xFFFFFFFF);

		for (int i = 0; i < 36; i++){
			if(i % 9 != 0){
				drawCompassSymbol(context, textRenderer, fov, "|", i * 10, camDirection, 0.05F, 0xFFFFFFFF, 1);
			}
		}

		//Add death position to the compass HUD
		if(deathPointBlockPos != null && CLIENT.world.getRegistryKey() == deathDimension && (boolean) SettingsManager.SHOW_LAST_DEATH_DIRECTION.getValue()){
			Vec3d playerPos = player.getPos();
			Vec3d deathPos = new Vec3d(deathPointBlockPos.getX(), 0, deathPointBlockPos.getZ());
			double dx = deathPos.x - playerPos.x;
			double dz = deathPos.z - playerPos.z;
			drawCompassSymbol(context, textRenderer, fov, "💀", (float)(MathHelper.atan2(dz, dx) * (180 / Math.PI)) - 90, camDirection, 0.05F, 0xFFFFFFFF);
		}

		//Add nether portal position to the compass HUD
		if(netherPortalBlockPos != null && CLIENT.world.getRegistryKey() == World.NETHER && (boolean) SettingsManager.SHOW_NETHER_PORTAL_DIRECTION.getValue()){
			Vec3d playerPos = player.getPos();
			Vec3d netherPortalPos = new Vec3d(netherPortalBlockPos.getX(), 0, netherPortalBlockPos.getZ());
			double dx = netherPortalPos.x - playerPos.x;
			double dz = netherPortalPos.z - playerPos.z;
			drawCompassSymbol(context, textRenderer, fov, "🌍", (float)(MathHelper.atan2(dz, dx) * (180 / Math.PI)) - 90, camDirection, 0.05F, 0xFFFFFFFF);
		}
	}

	public static void drawCompassSymbol(DrawContext context, TextRenderer textRenderer, float fov, String symbol, float targetDirection, float camDirection, float y, int color){
		drawCompassSymbol(context, textRenderer, fov, symbol, targetDirection, camDirection, y, color, 1.5F);
	}

	public static void drawCompassSymbol(DrawContext context, TextRenderer textRenderer, float fov, String symbol, float targetDirection, float camDirection, float y, int color, float scale){
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();

		//Determine the position of the symbol depending on the targetDirection and the camDirection
		int x = - (int) (screenWidth * 0.5 * GUI_WIDTH * MathHelper.wrapDegrees(camDirection-targetDirection)/fov);
		if(Math.abs(MathHelper.wrapDegrees(camDirection-targetDirection)/fov)>1.0){
			return;
		}

		//Adjust symbol transparency
		color = color % 16777216;
		int alpha = (int) (255 * (1-Math.abs(Math.sin(Math.toRadians(camDirection - targetDirection)))));
		color += 16777216 * alpha;
		
		//Scale the GUI
		var matrices = context.getMatrices();
		var before = new org.joml.Matrix3x2f(matrices);
		matrices.scale(scale, scale);

		context.drawCenteredTextWithShadow(textRenderer, Text.literal(symbol),(int) ((screenWidth/2 + x)/scale),(int) ((screenHeight*y)/scale - textRenderer.fontHeight/2f),color);

		matrices.set(before);
	}




	public static void saveValues() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(serverId).resolve(BetterCompass.MOD_ID + ".json");
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(valueMap, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private static void loadData() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(serverId).resolve(BetterCompass.MOD_ID + ".json");
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
			valueMap = GSON.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
			return;
        }

		Object raw = valueMap.get("deathDimension");
		if (raw instanceof Map<?, ?> rawMap) {
			Object valueRaw = rawMap.get("value");
			if(valueRaw instanceof Map<?, ?> innerRawMap){
				String namespace = (String) innerRawMap.get("namespace");
				String path = (String) innerRawMap.get("path");
				deathDimension = RegistryKey.<World>of(RegistryKeys.WORLD, Identifier.of(namespace,path));
			}
		}

		raw = valueMap.get("deathPointBlockPos");
		if (raw instanceof Map<?, ?> rawMap) {
			int x = ((Double) rawMap.get("x")).intValue();
			int y = ((Double) rawMap.get("y")).intValue();
			int z = ((Double) rawMap.get("z")).intValue();
			deathPointBlockPos = new BlockPos(x, y, z);
		}

		raw = valueMap.get("netherPortalBlockPos");
		if (raw instanceof Map<?, ?> rawMap) {
			int x = ((Double) rawMap.get("x")).intValue();
			int y = ((Double) rawMap.get("y")).intValue();
			int z = ((Double) rawMap.get("z")).intValue();
			netherPortalBlockPos = new BlockPos(x, y, z);
		}
    }
}