package com.hashiriyacarmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Partsページです。
 * メインページから遷移し、ここで各パーツに関する操作ができます。
 *
 * 前のページに戻るには ESC を押します。
 */
@OnlyIn(Dist.CLIENT)
public class PartsWrenchScreen extends WrenchGuiScreen {

    private final WrenchGuiScreen parentScreen;

    // ボタン情報をクラスフィールドで保持
    private int testButtonX, testButtonY, testButtonWidth, testButtonHeight;
    private int test2ButtonX, test2ButtonY, test2ButtonWidth, test2ButtonHeight;

    public PartsWrenchScreen(WrenchGuiScreen parent) {
        super();
        this.parentScreen = parent;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + IMAGE_WIDTH, this.topPos + IMAGE_HEIGHT,
                0x80000000);

        Component title = Component.literal("Parts");
        int titleX = this.leftPos + (IMAGE_WIDTH - this.font.width(title)) / 2;
        int titleY = this.topPos + 8;
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF);

        // ── testボタン ──
        Component testText = Component.literal("test");
        testButtonWidth = this.font.width(testText);
        testButtonHeight = this.font.lineHeight;
        testButtonX = this.leftPos + (IMAGE_WIDTH - testButtonWidth) / 2;
        testButtonY = this.topPos + 30;

        boolean hoveringTest = isMouseOver(mouseX, mouseY, testButtonX, testButtonY, testButtonWidth, testButtonHeight);
        int testColor = hoveringTest ? 0xFFFFFF : 0xCCCCCC;
        guiGraphics.drawString(this.font, testText, testButtonX, testButtonY, testColor);

        // ── test2ボタン ──
        Component test2Text = Component.literal("test2");
        test2ButtonWidth = this.font.width(test2Text);
        test2ButtonHeight = this.font.lineHeight;
        test2ButtonX = this.leftPos + (IMAGE_WIDTH - test2ButtonWidth) / 2;
        test2ButtonY = this.topPos + 30 + testButtonHeight + 5;

        boolean hoveringTest2 = isMouseOver(mouseX, mouseY, test2ButtonX, test2ButtonY, test2ButtonWidth, test2ButtonHeight);
        int test2Color = hoveringTest2 ? 0xFFFFFF : 0xCCCCCC;
        guiGraphics.drawString(this.font, test2Text, test2ButtonX, test2ButtonY, test2Color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int)mouseX, (int)mouseY, testButtonX, testButtonY, testButtonWidth, testButtonHeight)) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§6test button clicked!"),
                        false
                );
            }
            return true;
        }

        if (isMouseOver((int)mouseX, (int)mouseY, test2ButtonX, test2ButtonY, test2ButtonWidth, test2ButtonHeight)) {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§e[test2] This is test2 button!"),
                        false
                );
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(this.parentScreen);
            return true;
        }
        return true;
    }
}