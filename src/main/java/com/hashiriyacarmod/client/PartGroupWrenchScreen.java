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

    public PartGroupWrenchScreen(PartsWrenchScreen parent, String groupName) {
        super();
        this.parentScreen = parent;
        this.groupName = groupName != null ? groupName : "";

        labels.add("NONE");
        for (String baseName : PartRegistry.getBaseNamesForGroup(this.groupName)) {
            labels.add(PartRegistry.getDisplayName(baseName));
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

        // 中央：NONE または 登録 name
        String current = labels.get(selectedIndex);
        Component centerText = Component.literal(current);
        int textWidth = this.font.width(centerText);
        int textX = this.leftPos + (IMAGE_WIDTH - textWidth) / 2;
        int textY = this.topPos + 30;

        boolean hovering = mouseX >= textX && mouseX <= textX + textWidth
                && mouseY >= textY && mouseY <= textY + this.font.lineHeight;
        guiGraphics.drawString(this.font, centerText, textX, textY,
                hovering ? 0xFFFFFF : 0xCCCCCC);

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
            return true;
        }

        // ◀ 前へ
        Component arrow2 = Component.literal("◀");
        int arrowX2 = this.leftPos + 4;
        int arrowW2 = this.font.width(arrow2);
        if (mouseX >= arrowX2 && mouseX <= arrowX2 + arrowW2
                && mouseY >= rowY && mouseY <= rowY + lineH) {
            selectedIndex = (selectedIndex - 1 + labels.size()) % labels.size();
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