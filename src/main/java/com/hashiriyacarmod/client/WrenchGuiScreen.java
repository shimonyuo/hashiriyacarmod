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

        // ── ウィンドウの背景 ──
        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + IMAGE_WIDTH, this.topPos + IMAGE_HEIGHT,
                0x80000000);

        // ── タイトル（Parts） ──
        Component title = Component.literal("Vehicle");
        int titleX = this.leftPos + (IMAGE_WIDTH - this.font.width(title)) / 2;
        int titleY = this.topPos + 8;
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF);

        // ── testボタン ──
        Component testText = Component.literal("Parts");
        int textWidth = this.font.width(testText);
        int textHeight = this.font.lineHeight;
        int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
        int textY = this.topPos + 30;

        boolean hoveringTest = mouseX >= textX && mouseX <= textX + textWidth
                && mouseY >= textY && mouseY <= textY + textHeight;

        int textColor = hoveringTest ? 0xFFFFFF : 0xCCCCCC;
        guiGraphics.drawString(this.font, testText, textX, textY, textColor);

        // ← ここに今後、他のパーツボタンなどを追加できます

        // super.render()は呼ばない(親のWrenchGuiScreenの描画が重複するため)
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
            // ── Partsページに遷移します ──
            Minecraft.getInstance().setScreen(new PartsWrenchScreen(this));
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
}