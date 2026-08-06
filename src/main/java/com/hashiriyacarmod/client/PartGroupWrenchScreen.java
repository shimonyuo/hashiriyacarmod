package com.hashiriyacarmod.client;

import com.hashiriyacarmod.network.ModNetworking;
import com.hashiriyacarmod.network.SetAttachedPartPacket;
import com.hashiriyacarmod.parts.PartRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parts のグループをクリックした先の3ページ目。
 * NONE + その group のパーツ名を ◀▶ で切り替え表示（機能はまだ付けない）。
 */
@OnlyIn(Dist.CLIENT)
public class PartGroupWrenchScreen extends WrenchGuiScreen {

    private final PartsWrenchScreen parentScreen;
    private final String groupName;

    /** 0 = NONE, 1.. = パーツ */
    private int selectedIndex = 0;
    /** 表示用ラベル一覧（先頭は常に NONE） */
    private final List<String> labels = new ArrayList<>();
    private final List<String> baseNames = new ArrayList<>();
    private long marqueeStartMs = System.currentTimeMillis();

    public PartGroupWrenchScreen(PartsWrenchScreen parent, String groupName) {
        super();
        this.parentScreen = parent;
        this.groupName = groupName != null ? groupName : "";

        labels.add("NONE");
        baseNames.add(null);

        List<String> sorted = new ArrayList<>(PartRegistry.getBaseNamesForGroup(this.groupName));
        sorted.sort(String::compareToIgnoreCase); // baseName 名順

        for (String baseName : sorted) {
            labels.add(PartRegistry.getDisplayName(baseName));
            baseNames.add(baseName);
        }

        // NBT の AttachedParts から、この group の装着中パーツを初期表示
        this.selectedIndex = findInitialIndexFromNbt();
        this.marqueeStartMs = System.currentTimeMillis();
    }

    /**
     * この group に何か付いていればその baseName の index。
     * 何も無ければ 0（NONE = 未装着）。
     */
    private int findInitialIndexFromNbt() {
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        if (nbt == null || !nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_LIST)) {
            return 0;
        }

        var list = nbt.getList("AttachedParts", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String attached = list.getString(i);
            if (attached == null || attached.isBlank()) continue;

            // この group に属する装着パーツか
            List<String> groups = PartRegistry.getPartGroups(attached);
            if (!groups.contains(this.groupName)) continue;

            // アロー一覧の何番目か
            for (int idx = 1; idx < baseNames.size(); idx++) {
                if (attached.equals(baseNames.get(idx))) {
                    return idx;
                }
            }
        }
        // この group には何も付いていない → NONE
        return 0;
    }

    private void applyAttachedPartToLocalNbt(String group, String partBaseName) {
        if (WrenchGuiScreen.lastReceivedNbt == null) {
            WrenchGuiScreen.lastReceivedNbt = new net.minecraft.nbt.CompoundTag();
        }
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;

        java.util.List<String> attached = new java.util.ArrayList<>();
        if (nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_LIST)) {
            var list = nbt.getList("AttachedParts", net.minecraft.nbt.Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String name = list.getString(i);
                if (name == null || name.isBlank()) continue;
                // この group に属するものは一旦外す
                if (PartRegistry.getPartGroups(name).contains(group)) {
                    continue;
                }
                attached.add(name);
            }
        }

        if (partBaseName != null && !partBaseName.isBlank()
                && !"NONE".equalsIgnoreCase(partBaseName)) {
            attached.add(partBaseName.trim());
        }

        net.minecraft.nbt.ListTag newList = new net.minecraft.nbt.ListTag();
        for (String p : attached) {
            newList.add(net.minecraft.nbt.StringTag.valueOf(p));
        }
        nbt.put("AttachedParts", newList);
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

        // 中央：NONE または 登録 name
        String current = labels.get(selectedIndex);
        int textY = this.topPos + 30;
        int textH = this.font.lineHeight;
        int clipLeft = this.leftPos + 16;
        int clipRight = this.leftPos + IMAGE_WIDTH - 16;
        int maxTextW = clipRight - clipLeft;
        int fullW = this.font.width(current);

        boolean hoveringCenter = mouseX >= clipLeft && mouseX <= clipRight
                && mouseY >= textY && mouseY <= textY + textH;
        int color = hoveringCenter ? 0xFFFFFF : 0xCCCCCC;

        if (fullW <= maxTextW) {
            int textX = this.leftPos + (IMAGE_WIDTH - fullW) / 2;
            guiGraphics.drawString(this.font, current, textX, textY, color);
        } else {
            int gap = 24;
            int period = fullW + gap;
            long elapsed = System.currentTimeMillis() - this.marqueeStartMs;
            int scroll;
            if (elapsed < 1000L) {
                // 開始から1秒は先頭のまま待つ
                scroll = 0;
            } else {
                scroll = (int) (((elapsed - 1000L) / 40L) % period);
            }

            guiGraphics.enableScissor(clipLeft, textY - 1, clipRight, textY + textH + 1);
            int baseX = clipLeft - scroll;
            guiGraphics.drawString(this.font, current, baseX, textY, color);
            guiGraphics.drawString(this.font, current, baseX + period, textY, color);
            guiGraphics.disableScissor();
        }

        // ▶
        Component arrow = Component.literal("▶");
        int arrowX = this.leftPos + 120;
        int arrowY = this.topPos + 30;
        int arrowW = this.font.width(arrow);
        boolean hoveringArrow = mouseX >= arrowX && mouseX <= arrowX + arrowW
                && mouseY >= arrowY && mouseY <= arrowY + this.font.lineHeight;
        guiGraphics.drawString(this.font, arrow, arrowX, arrowY,
                hoveringArrow ? 0xFFFFFF : 0xCCCCCC);

        // ◀
        Component arrow2 = Component.literal("◀");
        int arrowX2 = this.leftPos + 4;
        int arrowY2 = this.topPos + 30;
        int arrowW2 = this.font.width(arrow2);
        boolean hoveringArrow2 = mouseX >= arrowX2 && mouseX <= arrowX2 + arrowW2
                && mouseY >= arrowY2 && mouseY <= arrowY2 + this.font.lineHeight;
        guiGraphics.drawString(this.font, arrow2, arrowX2, arrowY2,
                hoveringArrow2 ? 0xFFFFFF : 0xCCCCCC);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return true;

        int rowY = this.topPos + 30;
        int lineH = this.font.lineHeight;

        // ▶ 次へ
        Component arrow = Component.literal("▶");
        int arrowX = this.leftPos + 120;
        int arrowW = this.font.width(arrow);
        if (mouseX >= arrowX && mouseX <= arrowX + arrowW
                && mouseY >= rowY && mouseY <= rowY + lineH) {
            selectedIndex = (selectedIndex + 1) % labels.size();
            this.marqueeStartMs = System.currentTimeMillis();
            return true;
        }

        // ◀ 前へ
        Component arrow2 = Component.literal("◀");
        int arrowX2 = this.leftPos + 4;
        int arrowW2 = this.font.width(arrow2);
        if (mouseX >= arrowX2 && mouseX <= arrowX2 + arrowW2
                && mouseY >= rowY && mouseY <= rowY + lineH) {
            selectedIndex = (selectedIndex - 1 + labels.size()) % labels.size();
            this.marqueeStartMs = System.currentTimeMillis();
            return true;
        }

        int clipLeft = this.leftPos + 16;
        int clipRight = this.leftPos + IMAGE_WIDTH - 16;
        if (mouseX >= clipLeft && mouseX <= clipRight
                && mouseY >= rowY && mouseY <= rowY + lineH) {

            UUID carId = WrenchGuiScreen.lastReceivedCarUUID;
            if (carId == null) {
                return true;
            }

            String partBase = (selectedIndex <= 0)
                    ? "NONE"
                    : baseNames.get(selectedIndex);
            if (partBase == null) {
                partBase = "NONE";
            }

            // サーバーへ
            ModNetworking.sendToServer(
                    new SetAttachedPartPacket(carId, this.groupName, partBase)
            );

            // 手元の NBT も同じ内容に更新（メニューを閉じなくても次ページで反映）
            applyAttachedPartToLocalNbt(this.groupName, partBase);

            return true;
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

    public String getGroupName() {
        return this.groupName;
    }
}