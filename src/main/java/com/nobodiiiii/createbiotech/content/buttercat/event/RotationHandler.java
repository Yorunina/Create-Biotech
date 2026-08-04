package com.nobodiiiii.createbiotech.content.buttercat.event;

import com.nobodiiiii.createbiotech.content.buttercat.mob_effect.ButterRotationEffect;
import com.nobodiiiii.createbiotech.registry.CBMobEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;


public class RotationHandler {
    static float acceleration = 0;
    static int amplifier = -1;
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null ) return;
        MobEffectInstance effect = player.getEffect(CBMobEffects.BUTTER_ROTATION.get());

        acceleration = Mth.clamp(acceleration+0.1f*(effect!=null?1:-1),0,1);
        if(effect!=null){
            int amplifier0 = effect.getAmplifier();
            if(amplifier0 != amplifier)
                amplifier = amplifier0;
        }
    }

    public static void onRender(TickEvent.RenderTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || acceleration==0) return;

        float pt = event.renderTickTime;
        float y= player.getYRot()+ getAngle(pt,amplifier);

        player.setYRot(y);
    }
    public static float getAngle(float pt,int a) {
        return (getTickAngleSpeed(a) * pt ) % 360;
    }
    private static float getTickAngleSpeed(int amplifier){
        return (3*amplifier+1)* ButterRotationEffect.getRotationAngularSpeed() * acceleration;
    }
}

