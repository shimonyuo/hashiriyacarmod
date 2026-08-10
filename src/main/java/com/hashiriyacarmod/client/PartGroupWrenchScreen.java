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

    private final String groupName;

    /** 0 = NONE, 1.. = パーツ */
    private int selectedIndex = 0;
    /** 表示用ラベル一覧（先頭は常に NONE） */
    private final List<String> labels = new ArrayList<>();
    private final List<String> baseNames = new ArrayList<>();
    private long marqueeStartMs = System.currentTimeMillis();
    private final WrenchGuiScreen parentScreen;

    private final List<String> attachmentPath;


    public PartGroupWrenchScreen(WrenchGuiScreen parent, String groupName) {
        this(parent, groupName, List.of());
    }

    public PartGroupWrenchScreen(WrenchGuiScreen parent, String groupName, List<String> attachmentPath) {
        super();
        this.parentScreen = parent;
        this.groupName = groupName != null ? groupName : "";
        this.attachmentPath = attachmentPath != null
                ? List.copyOf(attachmentPath)
                : List.of();

        labels.add("NONE");
        baseNames.add(null);

        List<String> sorted = new ArrayList<>(PartRegistry.getBaseNamesForGroup(this.groupName));
        sorted.sort(String::compareToIgnoreCase);

        for (String baseName : sorted) {
            labels.add(PartRegistry.getDisplayName(baseName));
            baseNames.add(baseName);
        }

        this.selectedIndex = findInitialIndexFromNbt();
        this.marqueeStartMs = System.currentTimeMillis();
    }
    private static net.minecraft.nbt.CompoundTag getNodeAtPath(
            net.minecraft.nbt.CompoundTag attachedRoot, List<String> path) {
        net.minecraft.nbt.CompoundTag node = attachedRoot;
        if (path == null) return node;
        for (String t : path) {
            if (t == null || t.isBlank()) continue;
            if (!node.contains(t, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                return new net.minecraft.nbt.CompoundTag();
            }
            node = node.getCompound(t);
        }
        return node;
    }

    private int findInitialIndexFromNbt() {
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        if (nbt == null || !nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return 0;
        }
        net.minecraft.nbt.CompoundTag node = getNodeAtPath(
                nbt.getCompound("AttachedParts"), this.attachmentPath);

        for (String token : node.getAllKeys()) {
            if (token == null || token.isBlank()) continue;
            String base = com.hashiriyacarmod.CarEntity.attachedBaseName(token);
            String seat = com.hashiriyacarmod.CarEntity.attachedSeatGroup(token);
            boolean match = !seat.isEmpty()
                    ? seat.equals(this.groupName)
                    : PartRegistry.getPartGroups(base).contains(this.groupName);
            if (!match) continue;
            for (int idx = 1; idx < baseNames.size(); idx++) {
                if (base.equals(baseNames.get(idx))) return idx;
            }
        }
        return 0;
    }
    private void applyAttachedPartToLocalNbt(String group, String partBaseName) {
        if (WrenchGuiScreen.lastReceivedNbt == null) {
            WrenchGuiScreen.lastReceivedNbt = new net.minecraft.nbt.CompoundTag();
        }
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        net.minecraft.nbt.CompoundTag root = nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? nbt.getCompound("AttachedParts").copy()
                : new net.minecraft.nbt.CompoundTag();

        net.minecraft.nbt.CompoundTag parent = root;
        for (String pathToken : this.attachmentPath) {
            if (pathToken == null || pathToken.isBlank()) continue;
            if (!parent.contains(pathToken, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                parent.put(pathToken, new net.minecraft.nbt.CompoundTag());
            }
            parent = parent.getCompound(pathToken);
        }

        java.util.List<String> remove = new java.util.ArrayList<>();
        for (String token : parent.getAllKeys()) {
            String base = com.hashiriyacarmod.CarEntity.attachedBaseName(token);
            String seat = com.hashiriyacarmod.CarEntity.attachedSeatGroup(token);
            boolean match = !seat.isEmpty()
                    ? seat.equals(group)
                    : PartRegistry.getPartGroups(base).contains(group);
            if (match) remove.add(token);
        }
        for (String t : remove) parent.remove(t);

        if (partBaseName != null && !partBaseName.isBlank()
                && !"NONE".equalsIgnoreCase(partBaseName)) {
            String base = partBaseName.trim();
            List<String> partGroups = PartRegistry.getPartGroups(base);
            String token = partGroups.size() <= 1 ? base : base + ">" + group.trim();
            parent.put(token, new net.minecraft.nbt.CompoundTag());
        }

        nbt.put("AttachedParts", root);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        guiGraphics.fill(this.leftPos, this.topPos,
                this.leftPos + IMAGE_WIDTH, this.topPos + IMAGE_HEIGHT,
                0x80000000);

        Component title = Component.literal("Parts setting");
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

        if (hoveringCenter) {
            guiGraphics.renderTooltip(
                    this.font,
                    java.util.List.of(
                            Component.literal("Left-click: Equip part")
                    ),
                    java.util.Optional.empty(),
                    mouseX,
                    mouseY
            );
        }

        Component partsLink = Component.literal("Parts");
        int partsW = this.font.width(partsLink);
        int partsX = this.leftPos + (IMAGE_WIDTH - partsW) / 2;
        int partsY = this.topPos + 30 + this.font.lineHeight + 10;
        boolean hoveringParts = mouseX >= partsX && mouseX <= partsX + partsW
                && mouseY >= partsY && mouseY <= partsY + this.font.lineHeight;
        guiGraphics.drawString(this.font, partsLink, partsX, partsY,
                hoveringParts ? 0xFFFFFF : 0xCCCCCC);
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

            ModNetworking.sendToServer(
                    new SetAttachedPartPacket(
                            carId,
                            this.attachmentPath,
                            this.groupName,
                            partBase
                    )
            );
            applyAttachedPartToLocalNbt(this.groupName, partBase);

            // 手元の NBT も同じ内容に更新（メニューを閉じなくても次ページで反映）
            applyAttachedPartToLocalNbt(this.groupName, partBase);

            return true;
        }

        Component partsLink = Component.literal("Parts");
        int partsW = this.font.width(partsLink);
        int partsX = this.leftPos + (IMAGE_WIDTH - partsW) / 2;
        int partsY = this.topPos + 30 + this.font.lineHeight + 10;
        if (mouseX >= partsX && mouseX <= partsX + partsW
                && mouseY >= partsY && mouseY <= partsY + this.font.lineHeight) {

            String equippedBase = null;
            if (selectedIndex > 0 && selectedIndex < baseNames.size()) {
                equippedBase = baseNames.get(selectedIndex);
            }
            if (equippedBase == null || equippedBase.isBlank()) {
                equippedBase = findEquippedBaseFromNbt();
            }

            String parentToken = findEquippedTokenFromNbt();
            java.util.List<String> childPath = new java.util.ArrayList<>(this.attachmentPath);
            if (parentToken != null && !parentToken.isBlank()) {
                childPath.add(parentToken);
            }

            Minecraft.getInstance().setScreen(
                    new PartChildGroupsWrenchScreen(this, equippedBase, childPath));
            return true;
        }

        return true;
    }

    private String findEquippedTokenFromNbt() {
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        if (nbt == null || !nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        net.minecraft.nbt.CompoundTag node = getNodeAtPath(
                nbt.getCompound("AttachedParts"), this.attachmentPath);
        for (String token : node.getAllKeys()) {
            String base = com.hashiriyacarmod.CarEntity.attachedBaseName(token);
            String seat = com.hashiriyacarmod.CarEntity.attachedSeatGroup(token);
            boolean match = !seat.isEmpty()
                    ? seat.equals(this.groupName)
                    : PartRegistry.getPartGroups(base).contains(this.groupName);
            if (match) return token;
        }
        return null;
    }

    public List<String> getAttachmentPath() {
        return this.attachmentPath;
    }

    private String findEquippedBaseFromNbt() {
        net.minecraft.nbt.CompoundTag nbt = WrenchGuiScreen.lastReceivedNbt;
        if (nbt == null || !nbt.contains("AttachedParts", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        net.minecraft.nbt.CompoundTag node = getNodeAtPath(
                nbt.getCompound("AttachedParts"), this.attachmentPath);
        for (String token : node.getAllKeys()) {
            if (token == null || token.isBlank()) continue;
            String base = com.hashiriyacarmod.CarEntity.attachedBaseName(token);
            String seat = com.hashiriyacarmod.CarEntity.attachedSeatGroup(token);
            boolean match = !seat.isEmpty()
                    ? seat.equals(this.groupName)
                    : PartRegistry.getPartGroups(base).contains(this.groupName);
            if (match) return base;
        }
        return null;
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