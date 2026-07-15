package com.yision.allay.block.allayport;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.nobodiiiii.createbiotech.network.CBPackets;
import com.simibubi.create.content.trains.station.NoShadowFontWrapper;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import com.yision.allay.CreateAllay;
import com.yision.allay.logistics.courier.AllayCourierReturnMode;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AllayPortScreen extends AbstractSimiContainerScreen<AllayPortMenu> {
	private static final ResourceLocation ALLAY_PORT_GUI =
		CreateAllay.asResource("textures/gui/allay_port_gui.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int WINDOW_WIDTH = 220;
	private static final int WINDOW_HEIGHT = 82;
	private static final int BACKGROUND_X = 2;
	private static final int BACKGROUND_Y = 13;
	private static final int BACKGROUND_WIDTH = 218;
	private static final int BACKGROUND_HEIGHT = 116;
	private static final int PACKAGE_SLOTS_X = 25;
	private static final int PACKAGE_SLOTS_Y = 54;
	private static final int PACKAGE_SLOTS_SCREEN_X = 25;
	private static final int PACKAGE_SLOTS_SCREEN_Y = 7;
	private static final int BACKGROUND_SCREEN_X = PACKAGE_SLOTS_SCREEN_X - (PACKAGE_SLOTS_X - BACKGROUND_X);
	private static final int BACKGROUND_SCREEN_Y = PACKAGE_SLOTS_SCREEN_Y - (PACKAGE_SLOTS_Y - BACKGROUND_Y);
	private static final int ALLAY_SLOT_X = 9;
	private static final int ALLAY_SLOT_Y = 105;
	private static final int ALLAY_SLOT_SCREEN_X = BACKGROUND_SCREEN_X + ALLAY_SLOT_X - BACKGROUND_X;
	private static final int ALLAY_SLOT_SCREEN_Y = BACKGROUND_SCREEN_Y + ALLAY_SLOT_Y - BACKGROUND_Y;
	private static final int EDIT_NAME_X = 230;
	private static final int EDIT_NAME_Y = 3;
	private static final int EDIT_NAME_SIZE = 13;

	private EditBox addressBox;
	private IconButton confirmButton;
	private IconButton dontAcceptPackages;
	private IconButton acceptPackages;
	private IconButton[] returnModeButtons;
	private AllayCourierReturnMode selectedReturnMode;
	private ItemStack icon;
	private List<Rect2i> extraAreas = Collections.emptyList();
	private final List<Component> returnModeOptions = List.of(
		Component.translatable("gui.create_biotech.allay_port.return_mode.always_dock"),
		Component.translatable("gui.create_biotech.allay_port.return_mode.always_return"),
		Component.translatable("gui.create_biotech.allay_port.return_mode.return_when_unable"));

	public AllayPortScreen(AllayPortMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		icon = new ItemStack(menu.contentHolder.getBlockState()
			.getBlock()
			.asItem());
	}

	@Override
	protected void init() {
		setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT + AllGuiTextures.PLAYER_INVENTORY.getHeight());
		super.init();
		clearWidgets();

		int x = getGuiLeft();
		int y = getGuiTop();

		Consumer<String> onTextChanged;
		onTextChanged = s -> addressBox.setX(nameBoxX(s, addressBox));
		addressBox = new EditBox(new NoShadowFontWrapper(font), x + 23, y - 11, WINDOW_WIDTH - 20, 10,
			Component.empty());
		addressBox.setBordered(false);
		addressBox.setMaxLength(25);
		addressBox.setTextColor(0x3D3C48);
		addressBox.setValue(menu.contentHolder.addressFilter);
		addressBox.setFocused(false);
		addressBox.mouseClicked(0, 0, 0);
		addressBox.setResponder(onTextChanged);
		addressBox.setX(nameBoxX(addressBox.getValue(), addressBox));
		addRenderableWidget(addressBox);

		confirmButton =
			new IconButton(x + WINDOW_WIDTH - 33, y + WINDOW_HEIGHT - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> minecraft.player.closeContainer());
		addRenderableWidget(confirmButton);

		acceptPackages = new IconButton(x + 37, y + WINDOW_HEIGHT - 24, AllIcons.I_SEND_AND_RECEIVE);
		acceptPackages.withCallback(() -> {
			acceptPackages.green = true;
			dontAcceptPackages.green = false;
		});
		acceptPackages.green = menu.contentHolder.acceptsPackages;
		acceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_and_receive"));
		addRenderableWidget(acceptPackages);

		dontAcceptPackages = new IconButton(x + 37 + 18, y + WINDOW_HEIGHT - 24, AllIcons.I_SEND_ONLY);
		dontAcceptPackages.withCallback(() -> {
			acceptPackages.green = false;
			dontAcceptPackages.green = true;
		});
		dontAcceptPackages.green = !menu.contentHolder.acceptsPackages;
		dontAcceptPackages.setToolTip(CreateLang.translateDirect("gui.package_port.send_only"));
		addRenderableWidget(dontAcceptPackages);

		int returnModeX = x + 102;
		int returnModeY = y + WINDOW_HEIGHT - 24;
		returnModeButtons = new IconButton[] {
			createReturnModeButton(returnModeX, returnModeY, AllayCourierReturnMode.ALWAYS_DOCK, AllIcons.I_STOP),
			createReturnModeButton(returnModeX + 18, returnModeY, AllayCourierReturnMode.ALWAYS_RETURN,
				AllIcons.I_REFRESH),
			createReturnModeButton(returnModeX + 36, returnModeY, AllayCourierReturnMode.RETURN_WHEN_UNABLE,
				AllIcons.I_ROTATE_PLACE_RETURNED)
		};
		addRenderableWidgets(returnModeButtons);
		selectReturnMode(port().getReturnMode());

		containerTick();

		extraAreas = ImmutableList.of(new Rect2i(x + WINDOW_WIDTH, y + WINDOW_HEIGHT - 50, 70, 60));
	}

	private int nameBoxX(String s, EditBox nameBox) {
		return getGuiLeft() + WINDOW_WIDTH / 2 - (Math.min(font.width(s), nameBox.getWidth()) + 10) / 2;
	}

	private IconButton createReturnModeButton(int x, int y, AllayCourierReturnMode mode, AllIcons icon) {
		IconButton button = new IconButton(x, y, icon);
		button.withCallback(() -> selectReturnMode(mode));
		button.setToolTip(Component.translatable("gui.create_biotech.allay_port.return_mode")
			.append(": ")
			.append(returnModeOptions.get(mode.id())));
		return button;
	}

	private void selectReturnMode(AllayCourierReturnMode mode) {
		selectedReturnMode = mode;
		for (AllayCourierReturnMode option : AllayCourierReturnMode.values()) {
			returnModeButtons[option.id()].green = option == mode;
		}
	}

	@Override
	protected void containerTick() {
		acceptPackages.visible = menu.contentHolder.target != null;
		dontAcceptPackages.visible = menu.contentHolder.target != null;
		super.containerTick();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		int x = getGuiLeft();
		int y = getGuiTop();

		blitAllayPortGui(graphics, x + BACKGROUND_SCREEN_X, y + BACKGROUND_SCREEN_Y, BACKGROUND_X, BACKGROUND_Y,
			BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

		String text = addressBox.getValue();
		if (!addressBox.isFocused()) {
			if (addressBox.getValue()
				.isEmpty()) {
				text = icon.getHoverName()
					.getString();
				graphics.drawString(font, text, nameBoxX(text, addressBox), y - 11, 0x3D3C48, false);
			}
			blitAllayPortGui(graphics, nameBoxX(text, addressBox) + font.width(text) + 5, y - 14, EDIT_NAME_X,
				EDIT_NAME_Y, EDIT_NAME_SIZE, EDIT_NAME_SIZE);
		}

		GuiGameElement.of(icon).<GuiGameElement.GuiRenderBuilder>at(x + WINDOW_WIDTH + 6,
			y + WINDOW_HEIGHT - 56, -200)
			.scale(4)
			.render(graphics);

		int invX = leftPos + 30;
		int invY = topPos + 8 + imageHeight - AllGuiTextures.PLAYER_INVENTORY.getHeight();
		renderPlayerInventory(graphics, invX, invY);

		if (menu.contentHolder.target == null)
			return;

		x += ALLAY_SLOT_SCREEN_X;
		y += ALLAY_SLOT_SCREEN_Y;
		graphics.renderItem(menu.contentHolder.target.getIcon(), x + 1, y + 1);

		if (addressBox.isHovered()) {
			graphics.renderComponentTooltip(font, List.of(CreateLang.translate("gui.package_port.catch_packages")
				.color(AbstractSimiWidget.HEADER_RGB)
				.component(),
				CreateLang.translate("gui.package_port.catch_packages_empty")
					.style(ChatFormatting.GRAY)
					.component(),
				CreateLang.translate("gui.package_port.catch_packages_wildcard")
					.style(ChatFormatting.GRAY)
					.component()),
				mouseX, mouseY);
		}
	}

	private void blitAllayPortGui(GuiGraphics graphics, int x, int y, int u, int v, int width, int height) {
		graphics.blit(ALLAY_PORT_GUI, x, y, u, v, width, height, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean hitEnter = getFocused() instanceof EditBox
			&& (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER);

		if (hitEnter && addressBox.isFocused()) {
			addressBox.setFocused(false);
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		CBPackets.sendToServer(new AllayPortConfigurationPacket(menu.contentHolder.getBlockPos(),
			addressBox.getValue(), acceptPackages.green, selectedReturnMode));
		super.removed();
	}

	private AllayPortBlockEntity port() {
		return (AllayPortBlockEntity) menu.contentHolder;
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}
}
