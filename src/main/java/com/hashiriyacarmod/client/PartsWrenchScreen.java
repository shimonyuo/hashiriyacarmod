package com.hashiriyacarmod.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Partsページです。
 * メインページから遷移し、ここで各パーツに関する操作ができます。
 *
 * 前のページに戻るには ESC を押します。
 */
@OnlyIn(Dist.CLIENT)
public class PartsWrenchScreen extends WrenchGuiScreen {

    private final WrenchGuiScreen parentScreen;

    private List<String> allowedPartGroups = new ArrayList<>();
    private boolean waitingForServerData = true;

    // ボタン情報をクラスフィールドで保持
    private int testButtonX, testButtonY, testButtonWidth, testButtonHeight;
    private int test2ButtonX, test2ButtonY, test2ButtonWidth, test2ButtonHeight;

    public PartsWrenchScreen(WrenchGuiScreen parent) {
        super();
        this.parentScreen = parent;

        // WrenchGuiScreenから受け取ったデータを反映
        if (WrenchGuiScreen.lastReceivedGroups != null) {
            this.allowedPartGroups = new ArrayList<>(WrenchGuiScreen.lastReceivedGroups);
            this.waitingForServerData = false;
        }
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

        int startY = this.topPos + 30;
        int lineHeight = this.font.lineHeight + 6;  // Partsボタンと同じくらいの間隔

        if (waitingForServerData) {
            guiGraphics.drawString(this.font, "Loading parts...", this.leftPos + 40, startY, 0xAAAAAA);
        } else if (!allowedPartGroups.isEmpty()) {
            for (String group : allowedPartGroups) {
                Component groupText = Component.literal(group);
                int textWidth = this.font.width(groupText);
                int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;  // 中央寄せ
                int textY = startY;

                boolean hovering = mouseX >= textX && mouseX <= textX + textWidth
                        && mouseY >= textY && mouseY <= textY + this.font.lineHeight;

                int color = hovering ? 0xFFFFFF : 0xCCCCCC;   // Partsボタンと同じホバー色

                guiGraphics.drawString(this.font, groupText, textX, textY, color);

                startY += lineHeight;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingForServerData || allowedPartGroups.isEmpty()) {
            return true;
        }

        int startY = this.topPos + 30;
        int lineHeight = this.font.lineHeight + 6;

        for (String group : allowedPartGroups) {
            Component groupText = Component.literal(group);
            int textWidth = this.font.width(groupText);
            int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
            int textY = startY;

            boolean clicked = mouseX >= textX && mouseX <= textX + textWidth
                    && mouseY >= textY && mouseY <= textY + this.font.lineHeight;

            if (clicked) {
                Minecraft.getInstance().setScreen(new PartGroupWrenchScreen(this, group));
                return true;
            }

            startY += lineHeight;
        }

        return true;
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Minecraft.getInstance().setScreen(this.parentScreen);
            return true;
        }
        return true;
    }

    public void receivePartsInfo(List<String> groups) {
        this.allowedPartGroups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
        this.waitingForServerData = false;
    }
}