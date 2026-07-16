package com.yision.allay.client.gui.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class AllayCourierHudOverlay implements IGuiOverlay {
	public static final AllayCourierHudOverlay INSTANCE = new AllayCourierHudOverlay();

	private static final int RIGHT_PADDING = 6;
	private static final int TOP_PADDING = 6;
	private static final int LINE_GAP = 2;
	private static final int TEXT_COLOR = 0xFFE5F6FF;

	private static List<Integer> etaSeconds = List.of();
	private static ClientPacketListener activeConnection;

	private AllayCourierHudOverlay() {}

	public static void update(List<Integer> updatedEtaSeconds) {
		etaSeconds = updatedEtaSeconds == null ? List.of() : List.copyOf(updatedEtaSeconds);
		activeConnection = Minecraft.getInstance().getConnection();
	}

	@Override
	public void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
		Minecraft minecraft = Minecraft.getInstance();
		if (activeConnection != minecraft.getConnection()) {
			etaSeconds = List.of();
			activeConnection = minecraft.getConnection();
		}
		if (minecraft.player == null || minecraft.level == null) {
			etaSeconds = List.of();
			return;
		}
		if (minecraft.options.hideGui || minecraft.screen != null || etaSeconds.isEmpty()) {
			return;
		}

		int y = TOP_PADDING;
		for (int seconds : etaSeconds) {
			Component text = Component.translatable("gui.create_biotech.allay_port.arrival_eta", formatEta(seconds));
			int x = width - RIGHT_PADDING - minecraft.font.width(text);
			graphics.drawString(minecraft.font, text, x, y, TEXT_COLOR, true);
			y += minecraft.font.lineHeight + LINE_GAP;
		}
	}

	private static String formatEta(int seconds) {
		int minutes = seconds / 60;
		int remainderSeconds = seconds % 60;
		return String.format("%d:%02d", minutes, remainderSeconds);
	}
}
