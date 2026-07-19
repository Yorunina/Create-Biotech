package com.yision.allay.client.gui.hud;

import com.yision.allay.CreateAllay;
import com.yision.allay.logistics.courier.hud.AllayCourierHudEntry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class AllayCourierHudOverlay implements IGuiOverlay {
	public static final AllayCourierHudOverlay INSTANCE = new AllayCourierHudOverlay();

	private static final ResourceLocation ALLAY_PORT_GUI =
		CreateAllay.asResource("textures/gui/allay_port_gui.png");
	private static final int TEXTURE_SIZE = 256;
	private static final int RIGHT_PADDING = 6;
	private static final int TOP_PADDING = 6;
	private static final int LINE_GAP = 2;
	private static final long ANIMATION_TIME_MILLIS = 600L;
	private static final int TEXT_COLOR = 0x3D3C48;
	private static final int LABEL_LEFT_U = 27;
	private static final int LABEL_LEFT_V = 150;
	private static final int LABEL_LEFT_WIDTH = 46;
	private static final int LABEL_HEIGHT = 23;
	private static final int LABEL_MIDDLE_U = 77;
	private static final int LABEL_MIDDLE_V = 155;
	private static final int LABEL_MIDDLE_WIDTH = 24;
	private static final int LABEL_PANEL_HEIGHT = 18;
	private static final int LABEL_RIGHT_U = 105;
	private static final int LABEL_RIGHT_V = 155;
	private static final int LABEL_RIGHT_WIDTH = 24;
	private static final int LABEL_PANEL_Y = 5;
	private static final int TEXT_X = 28;
	private static final int TEXT_Y = 10;
	private static final int TEXT_RIGHT_PADDING = 5;

	private static final List<AnimatedEntry> animatedEntries = new ArrayList<>();
	private static ClientPacketListener activeConnection;

	private AllayCourierHudOverlay() {}

	public static void update(List<AllayCourierHudEntry> updatedEntries) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (activeConnection != connection) {
			animatedEntries.clear();
			activeConnection = connection;
		}
		List<AllayCourierHudEntry> incomingEntries =
			updatedEntries == null ? List.of() : List.copyOf(updatedEntries);
		long now = Util.getMillis();
		Set<UUID> incomingIds = new HashSet<>();

		for (AllayCourierHudEntry entry : incomingEntries) {
			incomingIds.add(entry.id());
			AnimatedEntry animatedEntry = findAnimatedEntry(entry.id());
			if (animatedEntry == null) {
				animatedEntries.add(new AnimatedEntry(entry, now));
			} else {
				animatedEntry.update(entry, now);
			}
		}
		for (AnimatedEntry animatedEntry : animatedEntries) {
			if (!incomingIds.contains(animatedEntry.entry().id())) {
				animatedEntry.hide(now);
			}
		}
	}

	@Override
	public void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
		Minecraft minecraft = Minecraft.getInstance();
		if (activeConnection != minecraft.getConnection()) {
			animatedEntries.clear();
			activeConnection = minecraft.getConnection();
		}
		if (minecraft.player == null || minecraft.level == null) {
			animatedEntries.clear();
			return;
		}
		long now = Util.getMillis();
		animatedEntries.removeIf(entry -> entry.finished(now));
		if (minecraft.options.hideGui || minecraft.screen != null || animatedEntries.isEmpty()) {
			return;
		}

		int y = TOP_PADDING;
		for (AnimatedEntry animatedEntry : animatedEntries) {
			AllayCourierHudEntry entry = animatedEntry.entry();
			Component address = entry.address().isBlank()
				? Component.translatable("block.create_biotech.allay_port")
				: Component.literal(entry.address());
			String translationKey = entry.incoming()
				? "gui.create_biotech.allay_port.arrival_eta.from"
				: "gui.create_biotech.allay_port.arrival_eta.to";
			Component text =
				Component.translatable(translationKey, address, formatEta(entry.etaSeconds()));
			renderLabel(graphics, minecraft, text, width, y, animatedEntry.visibility(now));
			y += LABEL_HEIGHT + LINE_GAP;
		}
	}

	private static void renderLabel(GuiGraphics graphics, Minecraft minecraft, Component text,
		int screenWidth, int y, float visibility) {
		int textWidth = minecraft.font.width(text);
		int middleWidth = Math.max(0,
			textWidth + TEXT_X + TEXT_RIGHT_PADDING - LABEL_LEFT_WIDTH - LABEL_RIGHT_WIDTH);
		int labelWidth = LABEL_LEFT_WIDTH + middleWidth + LABEL_RIGHT_WIDTH;
		int visibleX = screenWidth - RIGHT_PADDING - labelWidth;
		int x = Mth.floor(Mth.lerp(visibility, (float) screenWidth, (float) visibleX));

		graphics.blit(ALLAY_PORT_GUI, x, y, LABEL_LEFT_U, LABEL_LEFT_V,
			LABEL_LEFT_WIDTH, LABEL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

		int panelX = x + LABEL_LEFT_WIDTH;
		int remainingMiddleWidth = middleWidth;
		while (remainingMiddleWidth > 0) {
			int segmentWidth = Math.min(LABEL_MIDDLE_WIDTH, remainingMiddleWidth);
			graphics.blit(ALLAY_PORT_GUI, panelX, y + LABEL_PANEL_Y,
				LABEL_MIDDLE_U, LABEL_MIDDLE_V, segmentWidth, LABEL_PANEL_HEIGHT,
				TEXTURE_SIZE, TEXTURE_SIZE);
			panelX += segmentWidth;
			remainingMiddleWidth -= segmentWidth;
		}

		graphics.blit(ALLAY_PORT_GUI, panelX, y + LABEL_PANEL_Y,
			LABEL_RIGHT_U, LABEL_RIGHT_V, LABEL_RIGHT_WIDTH, LABEL_PANEL_HEIGHT,
			TEXTURE_SIZE, TEXTURE_SIZE);
		graphics.drawString(minecraft.font, text, x + TEXT_X, y + TEXT_Y, TEXT_COLOR, false);
	}

	private static AnimatedEntry findAnimatedEntry(UUID id) {
		for (AnimatedEntry animatedEntry : animatedEntries) {
			if (animatedEntry.entry().id().equals(id)) {
				return animatedEntry;
			}
		}
		return null;
	}

	private static String formatEta(int seconds) {
		int minutes = seconds / 60;
		int remainderSeconds = seconds % 60;
		return String.format("%d:%02d", minutes, remainderSeconds);
	}

	private static final class AnimatedEntry {
		private AllayCourierHudEntry entry;
		private long animationTime;
		private boolean hiding;

		private AnimatedEntry(AllayCourierHudEntry entry, long now) {
			this.entry = entry;
			animationTime = now;
		}

		private AllayCourierHudEntry entry() {
			return entry;
		}

		private void update(AllayCourierHudEntry updatedEntry, long now) {
			entry = updatedEntry;
			setHiding(false, now);
		}

		private void hide(long now) {
			setHiding(true, now);
		}

		private void setHiding(boolean shouldHide, long now) {
			if (hiding == shouldHide) {
				return;
			}
			animationTime = now - (long) ((1.0F - visibility(now)) * ANIMATION_TIME_MILLIS);
			hiding = shouldHide;
		}

		private float visibility(long now) {
			float progress = Mth.clamp((float) (now - animationTime) / ANIMATION_TIME_MILLIS, 0.0F, 1.0F);
			progress *= progress;
			return hiding ? 1.0F - progress : progress;
		}

		private boolean finished(long now) {
			return hiding && now - animationTime > ANIMATION_TIME_MILLIS;
		}
	}
}
