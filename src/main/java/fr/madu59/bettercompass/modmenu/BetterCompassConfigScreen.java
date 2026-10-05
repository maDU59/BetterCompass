package fr.madu59.bettercompass.modmenu;

import fr.madu59.bettercompass.config.SettingsManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class BetterCompassConfigScreen extends Screen {
    private static final String INDENT = " ⤷  ";
    private final Screen parent;
    private MyConfigListWidget list;

    protected BetterCompassConfigScreen(Screen parent) {
        super(Component.literal("Projectile Trajectory Preview Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        // Create the scrolling list
        this.list = new MyConfigListWidget(this.minecraft, this.width, this.height - 80, 40, 26);

        // Example: Add categories + buttons
        list.addCategory("better-compass.config.compass_hud");
        list.addButton(SettingsManager.SHOW_COMPASS_HUD, _ -> SettingsManager.SHOW_COMPASS_HUD.setToNextValue());
        list.addButton(SettingsManager.COMPASS_STYLE, _ -> SettingsManager.COMPASS_STYLE.setToNextValue(), INDENT);
        list.addButton(SettingsManager.COMPASS_POSITION, _ -> SettingsManager.COMPASS_POSITION.setToNextValue(), INDENT);
        list.addButton(SettingsManager.CARDINALS_DIRECTION_POSITION, _ -> SettingsManager.CARDINALS_DIRECTION_POSITION.setToNextValue());
        list.addButton(SettingsManager.CARDINALS_DIRECTION_COLOR, _ -> SettingsManager.CARDINALS_DIRECTION_COLOR.setToNextValue(), INDENT);
        list.addButton(SettingsManager.LAST_DEATH_DIRECTION_POSITION, _ -> SettingsManager.LAST_DEATH_DIRECTION_POSITION.setToNextValue());
        list.addButton(SettingsManager.LAST_DEATH_DIRECTION_COLOR, _ -> SettingsManager.LAST_DEATH_DIRECTION_COLOR.setToNextValue(), INDENT);
        list.addButton(SettingsManager.NETHER_PORTAL_DIRECTION_POSITION, _ -> SettingsManager.NETHER_PORTAL_DIRECTION_POSITION.setToNextValue());
        list.addButton(SettingsManager.NETHER_PORTAL_DIRECTION_COLOR, _ -> SettingsManager.NETHER_PORTAL_DIRECTION_COLOR.setToNextValue(), INDENT);

        Button doneButton = Button.builder(Component.literal("Done"), _ -> {
            this.minecraft.setScreenAndShow(this.parent);
            SettingsManager.saveSettings(SettingsManager.ALL_OPTIONS);
        }).bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();

        this.addRenderableWidget(this.list);
        this.addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
        SettingsManager.saveSettings(SettingsManager.ALL_OPTIONS);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.list.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}