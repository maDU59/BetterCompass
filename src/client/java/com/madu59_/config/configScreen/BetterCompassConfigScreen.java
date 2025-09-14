package com.maDU59_.config.configScreen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import com.maDU59_.config.SettingsManager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class BetterCompassConfigScreen extends Screen {
    private MyConfigListWidget list;

    private final String INDENT = " ⤷  ";

    protected BetterCompassConfigScreen(Screen parent) {
        super(Text.literal("Projectile Trajectory Preview Config"));
        this.parent = parent;
    }

    private final Screen parent;

    @Override
    protected void init() {
        super.init();
        // Create the scrolling list
        this.list = new MyConfigListWidget(this.client, this.width, this.height - 80, 40, 26);

        // Example: Add categories + buttons
        list.addCategory("COMPASS HUD");
        list.addButton(SettingsManager.SHOW_COMPASS_HUD, btn -> {
            SettingsManager.SHOW_COMPASS_HUD.setToNextValue();
        });
        list.addButton(SettingsManager.SHOW_LAST_DEATH_DIRECTION, btn -> {
            SettingsManager.SHOW_LAST_DEATH_DIRECTION.setToNextValue();
        }, INDENT);
        list.addButton(SettingsManager.SHOW_NETHER_PORTAL_DIRECTION, btn -> {
            SettingsManager.SHOW_NETHER_PORTAL_DIRECTION.setToNextValue();
        }, INDENT);

        ButtonWidget doneButton = ButtonWidget.builder(Text.literal("Done"), b -> {
            this.client.setScreen(this.parent);
            SettingsManager.saveSettings(SettingsManager.ALL_OPTIONS);
        }).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build();

        this.addDrawableChild(this.list);
        this.addDrawableChild(doneButton);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
        SettingsManager.saveSettings(SettingsManager.ALL_OPTIONS);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.list.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}