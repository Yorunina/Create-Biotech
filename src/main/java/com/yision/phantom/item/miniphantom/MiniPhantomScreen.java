package com.yision.phantom.item.miniphantom;

import com.simibubi.create.content.logistics.AddressEditBox;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import com.nobodiiiii.createbiotech.network.CBPackets;
import com.yision.phantom.network.phantom.MiniPhantomConfirmPacket;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MiniPhantomScreen extends AbstractSimiContainerScreen<MiniPhantomMenu> {
	private static final int GUI_WIDTH = 232;
	private static final int GUI_HEIGHT = 120;

	private AddressEditBox addressBox;
	private IconButton confirmButton;
	private List<Rect2i> extraAreas = List.of();

	public MiniPhantomScreen(MiniPhantomMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		setWindowSize(GUI_WIDTH, GUI_HEIGHT + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		super.init();
		clearWidgets();

		int x = getGuiLeft();
		int y = getGuiTop();
		extraAreas = List.of(new Rect2i(x + GUI_WIDTH, y + GUI_HEIGHT - 50, 70, 60));

		String previousAddress = addressBox == null ? menu.initialAddress : addressBox.getValue();
		addressBox = new AddressEditBox(this, new NoShadowFontWrapper(font), x + 55, y + 68, 110, 10, false);
		addressBox.setValue(previousAddress);
		addressBox.setTextColor(0x3D3C48);
		addRenderableWidget(addressBox);

		confirmButton = new IconButton(x + GUI_WIDTH - 30, y + GUI_HEIGHT - 25, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> CBPackets.sendToServer(new MiniPhantomConfirmPacket(addressBox.getValue())));
		addRenderableWidget(confirmButton);
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		addressBox.tick();
	}

	@Override
	protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int x = getGuiLeft();
		int y = getGuiTop();

		AllGuiTextures.REDSTONE_REQUESTER.render(graphics, x + 3, y);
		renderPlayerInventory(graphics, x + 25, y + 124);

		Component title = CreateLang.text(menu.openedStack.getHoverName().getString()).component();
		graphics.drawString(font, title, x + 117 - font.width(title) / 2, y + 4, 0x3D3C48, false);
		GuiGameElement.of(new ItemStack(menu.openedStack.getItem()))
			.<GuiGameElement.GuiRenderBuilder>at(x + 245, y + 80, 0)
			.scale(3)
			.render(graphics);

		if (addressBox.getValue().isBlank() && !addressBox.isFocused())
			graphics.drawString(font, CreateLang.translate("gui.stock_keeper.package_address")
				.style(ChatFormatting.ITALIC).component(), addressBox.getX(), addressBox.getY(), 0x8A8794, false);
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
