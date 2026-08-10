package com.hashiriyacarmod.client;

import com.hashiriyacarmod.parts.PartRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 装着中パーツが持つ子用 parts[] の group 一覧。
 * PartGroupWrenchScreen の "Parts" から開く。
 */
@OnlyIn(Dist.CLIENT)
public class PartChildGroupsWrenchScreen extends WrenchGuiScreen {

    private final PartGroupWrenchScreen parentScreen;
    private final String parentPartBase;
    /** 親パーツ・ノードまでのパス（子 seat 編集時に渡す） */
    private final List<String> attachmentPath;

    private final List<String> allowedPartGroups = new ArrayList<>();
    private static final int GROUPS_PER_PAGE = 10;
    private int currentPage = 0;

    public PartChildGroupsWrenchScreen(PartGroupWrenchScreen parent,
                                       String parentPartBase,
                                       List<String> attachmentPath) {
        super();
        this.parentScreen = parent;
        this.parentPartBase = parentPartBase != null ? parentPartBase : "";
        this.attachmentPath = attachmentPath != null
                ? List.copyOf(attachmentPath)
                : List.of();

        if (!this.parentPartBase.isEmpty()) {
            this.allowedPartGroups.addAll(
                    PartRegistry.getAllowedChildGroups(this.parentPartBase));
        }
    }

    /** path なし互換（使わない想定） */
    public PartChildGroupsWrenchScreen(PartGroupWrenchScreen parent, String parentPartBase) {
        this(parent, parentPartBase, List.of());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + IMAGE_WIDTH, this.topPos + IMAGE_HEIGHT,
                0x80000000);

        Component title = Component.literal("Part groups");
        int titleX = this.leftPos + (IMAGE_WIDTH - this.font.width(title)) / 2;
        guiGraphics.drawString(this.font, title, titleX, this.topPos + 8, 0xFFFFFF);

        int startY = this.topPos + 30;
        int lineHeight = this.font.lineHeight + 6;

        if (this.parentPartBase.isEmpty()) {
            guiGraphics.drawString(this.font, "No part equipped",
                    this.leftPos + 20, startY, 0xAAAAAA);
        } else if (allowedPartGroups.isEmpty()) {
            guiGraphics.drawString(this.font, "No sub groups",
                    this.leftPos + 30, startY, 0xAAAAAA);
        } else {
            clampPage();
            int from = currentPage * GROUPS_PER_PAGE;
            int to = Math.min(from + GROUPS_PER_PAGE, allowedPartGroups.size());
            for (int i = from; i < to; i++) {
                String group = allowedPartGroups.get(i);
                Component groupText = Component.literal(group);
                int textWidth = this.font.width(groupText);
                int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;

                boolean hovering = mouseX >= textX && mouseX <= textX + textWidth
                        && mouseY >= startY && mouseY <= startY + this.font.lineHeight;
                guiGraphics.drawString(this.font, groupText, textX, startY,
                        hovering ? 0xFFFFFF : 0xCCCCCC);
                startY += lineHeight;
            }
        }

        Component nav = Component.literal("◀");
        int navX = this.leftPos + 49;
        int navY = this.topPos + 30 + 10 * (this.font.lineHeight + 6);
        boolean hoveringNav = mouseX >= navX && mouseX <= navX + this.font.width(nav)
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight;
        guiGraphics.drawString(this.font, nav, navX, navY, hoveringNav ? 0xFFFFFF : 0xCCCCCC);

        Component navRight = Component.literal("▶");
        int navRightX = this.leftPos + 75;
        boolean hoveringNavRight = mouseX >= navRightX
                && mouseX <= navRightX + this.font.width(navRight)
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight;
        guiGraphics.drawString(this.font, navRight, navRightX, navY,
                hoveringNavRight ? 0xFFFFFF : 0xCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int navY = this.topPos + 30 + 10 * (this.font.lineHeight + 6);

        Component nav = Component.literal("◀");
        int navX = this.leftPos + 49;
        if (mouseX >= navX && mouseX <= navX + this.font.width(nav)
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight) {
            int total = getTotalPages();
            currentPage = (currentPage - 1 + total) % total;
            return true;
        }

        Component navRight = Component.literal("▶");
        int navRightX = this.leftPos + 75;
        if (mouseX >= navRightX && mouseX <= navRightX + this.font.width(navRight)
                && mouseY >= navY && mouseY <= navY + this.font.lineHeight) {
            currentPage = (currentPage + 1) % getTotalPages();
            return true;
        }

        if (allowedPartGroups.isEmpty()) return true;

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

            if (mouseX >= textX && mouseX <= textX + textWidth
                    && mouseY >= startY && mouseY <= startY + this.font.lineHeight) {
                Minecraft.getInstance().setScreen(
                        new PartGroupWrenchScreen(this, group, this.attachmentPath));
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
}