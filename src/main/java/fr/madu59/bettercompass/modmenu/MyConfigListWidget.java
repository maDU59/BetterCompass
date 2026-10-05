package fr.madu59.bettercompass.modmenu;

import fr.madu59.bettercompass.config.Option;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class MyConfigListWidget extends ContainerObjectSelectionList<MyConfigListWidget.Entry> {
    public MyConfigListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    @Override
	protected int scrollBarX() {
		return this.getX() + this.getWidth() - 6;
	}

    @Override
    public int getRowWidth() {
        return this.width;
    }

    public void addCategory(String name) {
        this.addEntry(new CategoryEntry(name));
    }

    public void addButton(String name, Button.OnPress onPress) {
        this.addEntry(new ButtonEntry(Button.builder(Component.literal(name), onPress).bounds(0, 0, 100, 20).build(), null, ""));
    }

    public void addButton(Option option, Button.OnPress onPress) {
        this.addEntry(new ButtonEntry(Button.builder(Component.literal(option.getValueAsString()), onPress).bounds(0, 0, 100, 20).build(), option, ""));
    }

    public void addButton(Option option, Button.OnPress onPress, String indent) {
        this.addEntry(new ButtonEntry(Button.builder(Component.literal(option.getValueAsString()), onPress).bounds(0, 0, 100, 20).build(), option, indent));
    }

    // Base entry
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<MyConfigListWidget.Entry> {}

    // Category header
    public static class CategoryEntry extends MyConfigListWidget.Entry {
        private final String name;

        public CategoryEntry(String name) {
            this.name = name;
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            Font textRenderer = Minecraft.getInstance().font;
            int textX = getContentX() + getContentWidth() / 2;
            int textY = getContentY() + (getContentHeight() - textRenderer.lineHeight) / 2;
            graphics.centeredText(textRenderer, Component.translatable(this.name), textX, textY, 0xFFFFFFFF);
        }  

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of();
        }
    }

    // Button entry
    public static class ButtonEntry extends MyConfigListWidget.Entry{
        private final Button button;
        private final String name;
        private final String description;
        private final String indent;
        private final Option option;

        public ButtonEntry(Button button, Option option, String indent) {
            this.button = button;
            this.name = option.getName();
            this.description = option.getDescription();
            this.indent = indent;
            this.option = option;
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.button.setY(this.getContentY() + (this.getContentHeight() - this.button.getHeight()) / 2);
            this.button.setX(this.getContentWidth() - this.button.getWidth() - 10);
            this.button.extractRenderState(graphics, mouseX, mouseY, a);

            if(this.description == null) return;

            Font textRenderer = Minecraft.getInstance().font;
            graphics.text(textRenderer, Component.literal(indent + this.name), 10, this.getContentY() + (this.getContentHeight() - textRenderer.lineHeight) / 2, 0xFFFFFFFF, true);
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of(this.button);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of(this.button);
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubleClick) {
            if (!this.button.mouseClicked(click, doubleClick)) return false;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (this.option != null)this.button.setMessage(Component.literal(this.option.getValueAsString()));
            return true;
        }
    }
}