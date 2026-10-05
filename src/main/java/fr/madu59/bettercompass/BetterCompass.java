package fr.madu59.bettercompass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.madu59.bettercompass.config.SettingsManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class BetterCompass implements ClientModInitializer {
	public static final String MOD_ID = "better-compass";

	private static final Minecraft CLIENT = Minecraft.getInstance();
	private static final float GUI_WIDTH = 0.5F;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Map<String, String> valueMap = new LinkedHashMap<>();

	private static ResourceKey<Level> lastDimension = null;

	public static BlockPos deathPointBlockPos = null;
	public static ResourceKey<Level> deathDimension = null;
	public static BlockPos netherPortalBlockPos = null;
	public static String serverId;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> dispatcher.register(ClientCommands.literal("bettercompass")
				.then(ClientCommands.argument("option", StringArgumentType.string()).suggests((_, builder) -> SharedSuggestionProvider.suggest(SettingsManager.getAllOptionsId(), builder))
						.then(ClientCommands.argument("value", StringArgumentType.string()).suggests((context, builder) -> SharedSuggestionProvider.suggest(SettingsManager.getOptionPossibleValues(StringArgumentType.getString(context, "option")), builder))
								.executes(context -> {
									String option = StringArgumentType.getString(context, "option");
									String value = StringArgumentType.getString(context, "value");

									boolean success = SettingsManager.setOptionValue(option, value);
									context.getSource().sendFeedback(Component.literal(success ? "Updated " + option + " to " + value : "Failed to update setting."));
									if(success){
										SettingsManager.saveSettings(SettingsManager.ALL_OPTIONS);
									}
									return success ? 1 : 0;
								})
						)
				)
		));

		// Attach our rendering code to before the chat hud layer. Our layer will render right before the chat. The API will take care of z spacing.
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath(MOD_ID, "before_chat"), BetterCompass::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null && client.player.isDeadOrDying()) {
				deathPointBlockPos = client.player.blockPosition();
				deathDimension = client.level.dimension();
				valueMap.put("deathPointBlockPos", blockPosToString(deathPointBlockPos));
				valueMap.put("deathDimension", registryKeyToString(deathDimension));
				saveValues();
			}
			if (client.level != null) {
			ResourceKey<Level> current = client.level.dimension();
				if (lastDimension != null && !lastDimension.equals(current)) {
					if (current == Level.NETHER) {
						netherPortalBlockPos = client.player.blockPosition();
						valueMap.put("netherPortalBlockPos", blockPosToString(netherPortalBlockPos));
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

			if (CLIENT.getSingleplayerServer() == null) {
				// Multiplayer
				ServerData info = CLIENT.getCurrentServer();
				serverId = info != null ? info.ip.replace(":", "_") : "unknown_server";
			} else {
				// Singleplayer
				serverId = CLIENT.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
					.getParent().getFileName().toString();
			}
			loadData();
		});
	}

	private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		float compassPosition = 0.05F;
		if(SettingsManager.COMPASS_POSITION.getValueAsString().equals("Bottom")){compassPosition = 1-0.18F;}
		float deltaY = 0F;
		float size = 1.5F;
		int color;
		Player player = CLIENT.player;
		int optionValueIndex = SettingsManager.SHOW_COMPASS_HUD.getValueAsIndex();
		if(optionValueIndex == 3 
			|| (optionValueIndex == 2 && !player.getMainHandItem().getItem().getDescriptionId().equals("item.minecraft.compass"))
			|| (optionValueIndex == 1 && !player.getInventory().hasAnyOf(Set.of(Items.COMPASS)))
		){
			return;
		}
		Font textRenderer = Minecraft.getInstance().font;
		Camera camera = CLIENT.gameRenderer.mainCamera();

        float fov = camera.getFov();
		float camDirection = camera.yRot();

		if(!SettingsManager.CARDINALS_DIRECTION_POSITION.getValue().equals("Disabled")){
			color = SettingsManager.getRGBColorFromSetting(SettingsManager.CARDINALS_DIRECTION_COLOR.getValueAsString());
			if(SettingsManager.CARDINALS_DIRECTION_POSITION.getValue().equals("Under")){deltaY = 0.03F; size = 0.8F;}
			if(SettingsManager.CARDINALS_DIRECTION_POSITION.getValue().equals("Above")){deltaY = -0.03F; size = 0.8F;}
			drawCompassSymbol(context, textRenderer, fov, "N", 180, camDirection, compassPosition + deltaY, color, size);
			drawCompassSymbol(context, textRenderer, fov, "E", 270, camDirection, compassPosition + deltaY, color, size);
			drawCompassSymbol(context, textRenderer, fov, "S", 0, camDirection, compassPosition + deltaY, color, size);
			drawCompassSymbol(context, textRenderer, fov, "W", 90, camDirection, compassPosition + deltaY, color, size);
		}
			
		for (int i = 0; i < 36; i++){
			if(i % 9 != 0 || !SettingsManager.CARDINALS_DIRECTION_POSITION.getValue().equals("Aligned")){
				drawCompassSymbol(context, textRenderer, fov, "|", i * 10, camDirection, compassPosition, 0xFFFFFFFF, 1);
			}
		}

		//Add death position to the compass HUD
		if(deathPointBlockPos != null && CLIENT.level.dimension() == deathDimension && !SettingsManager.LAST_DEATH_DIRECTION_POSITION.getValue().equals("Disabled")){
			deltaY = 0;
			size = 1.5F;
			color = SettingsManager.getRGBColorFromSetting(SettingsManager.LAST_DEATH_DIRECTION_COLOR.getValueAsString());
			if(SettingsManager.LAST_DEATH_DIRECTION_POSITION.getValue().equals("Under")){deltaY = 0.03F; size = 0.8F;}
			if(SettingsManager.LAST_DEATH_DIRECTION_POSITION.getValue().equals("Above")){deltaY = -0.03F; size = 0.8F;}
			Vec3 deathPos = new Vec3(deathPointBlockPos.getX(), 0, deathPointBlockPos.getZ());
			double dx = deathPos.x - player.getX();
			double dz = deathPos.z - player.getZ();
			drawCompassSymbol(context, textRenderer, fov, "💀", (float)(Mth.atan2(dz, dx) * (180 / Math.PI)) - 90, camDirection, compassPosition + deltaY, color, size);
		}

		//Add nether portal position to the compass HUD
		if(netherPortalBlockPos != null && CLIENT.level.dimension() == Level.NETHER && !SettingsManager.NETHER_PORTAL_DIRECTION_POSITION.getValue().equals("Disabled")){
			deltaY = 0;
			size = 1.5F;
			color = SettingsManager.getRGBColorFromSetting(SettingsManager.NETHER_PORTAL_DIRECTION_COLOR.getValueAsString());
			if(SettingsManager.NETHER_PORTAL_DIRECTION_POSITION.getValue().equals("Under")){deltaY = 0.03F; size = 0.8F;}
			if(SettingsManager.NETHER_PORTAL_DIRECTION_POSITION.getValue().equals("Above")){deltaY = -0.03F; size = 0.8F;}
			Vec3 netherPortalPos = new Vec3(netherPortalBlockPos.getX(), 0, netherPortalBlockPos.getZ());
			double dx = netherPortalPos.x - player.getX();
			double dz = netherPortalPos.z - player.getZ();
			drawCompassSymbol(context, textRenderer, fov, "🌍", (float)(Mth.atan2(dz, dx) * (180 / Math.PI)) - 90, camDirection, compassPosition + deltaY, color, size);
		}
	}

	public static void drawCompassSymbol(GuiGraphicsExtractor context, Font textRenderer, float fov, String symbol, float targetDirection, float camDirection, float y, int color, float scale){
		int screenWidth = context.guiWidth();
		int screenHeight = context.guiHeight();

		//Determine the position of the symbol depending on the targetDirection and the camDirection
		int x = - (int) (screenWidth * 0.5 * GUI_WIDTH * Mth.wrapDegrees(camDirection - targetDirection) / fov);
		if (Math.abs(Mth.wrapDegrees(camDirection - targetDirection) / fov)>1.0){
			return;
		}

		//Adjust symbol transparency
		color = color % 16777216;
		int alpha = (int) (255 * (1-Math.abs(Math.sin(Math.toRadians(camDirection - targetDirection)))));
		color += 16777216 * alpha;
		
		//Scale the GUI
		var matrices = context.pose();
		var before = new Matrix3x2f(matrices);
		matrices.scale(scale, scale);

		if (SettingsManager.COMPASS_STYLE.getValueAsString().equals("Shadows")) {
			context.centeredText(textRenderer, Component.literal(symbol), (int) ((screenWidth / 2f + x) / scale), (int) ((screenHeight * y) / scale - textRenderer.lineHeight / 2f), color);
		} else {
			context.text(textRenderer, symbol,(int) ((screenWidth / 2f + x) / scale - textRenderer.width(symbol) / 2f), (int) ((screenHeight * y)/scale - textRenderer.lineHeight / 2f), color, false);
		}
		
		matrices.set(before);
	}

	public static void saveValues() {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(serverId).resolve(MOD_ID + ".json");
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
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(serverId).resolve(MOD_ID + ".json");
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
			valueMap = GSON.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
			return;
        }

		if(valueMap.containsKey("deathPointBlockPos")){
			deathPointBlockPos = stringToBlockPos(valueMap.get("deathPointBlockPos"));
		}
		if(valueMap.containsKey("deathDimension")){
			deathDimension = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(valueMap.get("deathDimension")));
		}
		if(valueMap.containsKey("netherPortalBlockPos")){
			netherPortalBlockPos = stringToBlockPos(valueMap.get("netherPortalBlockPos"));
		}
    }

	private static String blockPosToString(BlockPos pos){
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	private static String registryKeyToString(ResourceKey<Level> key){
		return key.identifier().toString();
	}

	private static BlockPos stringToBlockPos(String str){
		String[] coords = str.split(",");
		return new BlockPos(
			Integer.parseInt(coords[0]),
			Integer.parseInt(coords[1]),
			Integer.parseInt(coords[2])
		);
	}
}