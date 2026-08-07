package com.hashiriyacarmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.UUID;

/**
 * レンチで車のOBBを検知した時に開くGUI画面です。（メインページ）
 * 複数のページを切り替えて表示します。
 */
@OnlyIn(Dist.CLIENT)
public class WrenchGuiScreen extends Screen {

    public static CompoundTag lastReceivedNbt = null;
    public static UUID lastReceivedCarUUID = null;

    public static final int IMAGE_WIDTH = 130;
    public static final int IMAGE_HEIGHT = 200;
    public static final int WINDOW_X = 10;
    public static final int WINDOW_Y = 10;

    protected int leftPos;
    protected int topPos;

    public static List<String> lastReceivedGroups = null;

    public static boolean expectGuiFromServer = false;

    public WrenchGuiScreen() {
        super(Component.literal("Wrench GUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = WINDOW_X;
        this.topPos = WINDOW_Y;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + IMAGE_WIDTH, this.topPos + IMAGE_HEIGHT,
                0x80000000);

        Component title = Component.literal("Vehicle");
        int titleX = this.leftPos + (IMAGE_WIDTH - this.font.width(title)) / 2;
        int titleY = this.topPos + 8;
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF);

        Component testText = Component.literal("Parts");
        int textWidth = this.font.width(testText);
        int textHeight = this.font.lineHeight;
        int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
        int textY = this.topPos + 30;

        boolean hoveringTest = mouseX >= textX && mouseX <= textX + textWidth
                && mouseY >= textY && mouseY <= textY + textHeight;

        int textColor = hoveringTest ? 0xFFFFFF : 0xCCCCCC;
        guiGraphics.drawString(this.font, testText, textX, textY, textColor);

        Component colorsText = Component.literal("Colors");
        int colorsWidth = this.font.width(colorsText);
        int colorsHeight = this.font.lineHeight;
        int colorsX = this.leftPos + (IMAGE_WIDTH - colorsWidth) / 2;
        int colorsY = textY + textHeight + 6;

        boolean hoveringColors = mouseX >= colorsX && mouseX <= colorsX + colorsWidth
                && mouseY >= colorsY && mouseY <= colorsY + colorsHeight;

        int colorsColor = hoveringColors ? 0xFFFFFF : 0xCCCCCC;
        guiGraphics.drawString(this.font, colorsText, colorsX, colorsY, colorsColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        Component partsText = Component.literal("Parts");
        int textWidth = this.font.width(partsText);
        int textHeight = this.font.lineHeight;
        int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
        int textY = this.topPos + 30;

        boolean clickedOnParts = mouseX >= textX && mouseX <= textX + textWidth
                && mouseY >= textY && mouseY <= textY + textHeight;

        if (clickedOnParts) {
            Minecraft.getInstance().setScreen(new PartsWrenchScreen(this));
            return true;
        }

        Component colorsText = Component.literal("Colors");
        int colorsWidth = this.font.width(colorsText);
        int colorsHeight = this.font.lineHeight;
        int colorsX = this.leftPos + (IMAGE_WIDTH - colorsWidth) / 2;
        int colorsY = textY + textHeight + 6;

        boolean clickedOnColors = mouseX >= colorsX && mouseX <= colorsX + colorsWidth
                && mouseY >= colorsY && mouseY <= colorsY + colorsHeight;

        if (clickedOnColors) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§eColors (準備中)"),
                        false
                );
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        lastReceivedNbt = null;
        lastReceivedCarUUID = null;
        lastReceivedGroups = null;
    }
}