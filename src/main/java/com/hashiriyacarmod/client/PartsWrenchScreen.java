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

    private static final int GROUPS_PER_PAGE = 10;
    /** 0 始まり */
    private int currentPage = 0;

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

        Component title = Component.literal("Parts groups");
        int titleX = this.leftPos + (IMAGE_WIDTH - this.font.width(title)) / 2;
        int titleY = this.topPos + 8;
        guiGraphics.drawString(this.font, title, titleX, titleY, 0xFFFFFF);

        int startY = this.topPos + 30;
        int lineHeight = this.font.lineHeight + 6;  // Partsボタンと同じくらいの間隔

        if (waitingForServerData) {
            guiGraphics.drawString(this.font, "Loading parts...", this.leftPos + 40, startY, 0xAAAAAA);
        } else if (!allowedPartGroups.isEmpty()) {
            clampPage();
            int from = currentPage * GROUPS_PER_PAGE;
            int to = Math.min(from + GROUPS_PER_PAGE, allowedPartGroups.size());
            for (int i = from; i < to; i++) {
                String group = allowedPartGroups.get(i);
                Component groupText = Component.literal(group);
                int textWidth = this.font.width(groupText);
                int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
                int textY = startY;

                boolean hovering = mouseX >= textX && mouseX <= textX + textWidth
                        && mouseY >= textY && mouseY <= textY + this.font.lineHeight;

                int color = hovering ? 0xFFFFFF : 0xCCCCCC;

                guiGraphics.drawString(this.font, groupText, textX, textY, color);

                startY += lineHeight;
            }
        }

        Component nav = Component.literal("◀");
        int navX = this.leftPos + 49;
        int navY = this.topPos + 30 + 10 * (this.font.lineHeight + 6);
        int navW = this.font.width(nav);
        boolean hoveringNav = mouseX >= navX && mouseX <= navX + navW
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight;
        guiGraphics.drawString(this.font, nav, navX, navY,
                hoveringNav ? 0xFFFFFF : 0xCCCCCC);

        Component navRight = Component.literal("▶");
        int navRightX = this.leftPos + 75;
        int navRightW = this.font.width(navRight);
        boolean hoveringNavRight = mouseX >= navRightX && mouseX <= navRightX + navRightW
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight;
        guiGraphics.drawString(this.font, navRight, navRightX, navY,
                hoveringNavRight ? 0xFFFFFF : 0xCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Component nav = Component.literal("◀");
        int navX = this.leftPos + 49;
        int navY = this.topPos + 30 + 10 * (this.font.lineHeight + 6);
        int navW = this.font.width(nav);
        if (mouseX >= navX && mouseX <= navX + navW
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight) {
            int total = getTotalPages();
            currentPage = (currentPage - 1 + total) % total;
            return true;
        }

        // ▶ 次へ（最後のさらに次 → 最初へ）
        Component navRight = Component.literal("▶");
        int navRightX = this.leftPos + 75;
        int navRightW = this.font.width(navRight);
        if (mouseX >= navRightX && mouseX <= navRightX + navRightW
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight) {
            int total = getTotalPages();
            currentPage = (currentPage + 1) % total;
            return true;
        }

        if (waitingForServerData || allowedPartGroups.isEmpty()) {
            return true;
        }

        clampPage();
        int startY = this.topPos + 30;
        int lineHeight = this.font.lineHeight + 6;
        int from = currentPage * GROUPS_PER_PAGE;
        int to = Math.min(from + GROUPS_PER_PAGE, allowedPartGroups.size());
        for (int i = from; i < to; i++) {
            String group = allowedPartGroups.get(i);
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

    private int getTotalPages() {
        if (allowedPartGroups.isEmpty()) return 1;
        return (allowedPartGroups.size() + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE;
    }

    private void clampPage() {
        int max = Math.max(0, getTotalPages() - 1);
        if (currentPage < 0) currentPage = 0;
        if (currentPage > max) currentPage = max;
    }

    public void receivePartsInfo(List<String> groups) {
        this.allowedPartGroups = groups != null ? new ArrayList<>(groups) : new ArrayList<>();
        this.waitingForServerData = false;
    }
}