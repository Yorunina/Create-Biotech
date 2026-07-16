package com.nobodiiiii.createbiotech.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Vanilla note particle behavior rendered at half of the vanilla note's visual size. */
public class HalfScaleNoteParticle extends TextureSheetParticle {

	private static final float VANILLA_NOTE_SCALE = 1.5f;
	private static final float COURIER_TRAIL_SCALE = 0.5f;

	protected HalfScaleNoteParticle(ClientLevel level, double x, double y, double z, double color) {
		super(level, x, y, z, 0.0, 0.0, 0.0);
		friction = 0.66f;
		speedUpWhenYMotionIsBlocked = true;
		xd *= 0.01f;
		yd *= 0.01f;
		zd *= 0.01f;
		yd += 0.2;
		rCol = Math.max(0.0f, Mth.sin(((float) color) * Mth.TWO_PI) * 0.65f + 0.35f);
		gCol = Math.max(0.0f, Mth.sin(((float) color + 1.0f / 3.0f) * Mth.TWO_PI) * 0.65f + 0.35f);
		bCol = Math.max(0.0f, Mth.sin(((float) color + 2.0f / 3.0f) * Mth.TWO_PI) * 0.65f + 0.35f);
		quadSize *= VANILLA_NOTE_SCALE * COURIER_TRAIL_SCALE;
		lifetime = 6;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}

	@Override
	public float getQuadSize(float partialTick) {
		return quadSize * Mth.clamp(((float) age + partialTick) / lifetime * 32.0f, 0.0f, 1.0f);
	}

	public static class Provider implements ParticleProvider<SimpleParticleType> {

		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level,
			double x, double y, double z, double color, double ySpeed, double zSpeed) {
			HalfScaleNoteParticle particle = new HalfScaleNoteParticle(level, x, y, z, color);
			particle.pickSprite(sprites);
			return particle;
		}
	}
}
