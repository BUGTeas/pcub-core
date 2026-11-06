package org.pcub.core.geyser.item;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;

import java.util.List;
import java.util.Locale;

public enum Potion {
    WATER,
    MUNDANE, // 2 is extended?
    THICK,
    AWKWARD,
    NIGHT_VISION(effect(Effect.NIGHT_VISION,0,3600)),
    LONG_NIGHT_VISION(effect(Effect.NIGHT_VISION,0,9600)),
    INVISIBILITY(effect(Effect.INVISIBILITY,0,3600)),
    LONG_INVISIBILITY(effect(Effect.INVISIBILITY,0,9600)),
    LEAPING(effect(Effect.JUMP_BOOST,0,3600)),
    LONG_LEAPING(effect(Effect.JUMP_BOOST,0,9600)),
    STRONG_LEAPING(effect(Effect.JUMP_BOOST,1,1800)),
    FIRE_RESISTANCE(effect(Effect.FIRE_RESISTANCE,0,3600)),
    LONG_FIRE_RESISTANCE(effect(Effect.FIRE_RESISTANCE,0,9600)),
    SWIFTNESS(effect(Effect.SPEED,0,3600)),
    LONG_SWIFTNESS(effect(Effect.SPEED,0,9600)),
    STRONG_SWIFTNESS(effect(Effect.SPEED,1,1800)),
    SLOWNESS(effect(Effect.SLOWNESS,0,1800)),
    LONG_SLOWNESS(effect(Effect.SLOWNESS,0,4800)),
    STRONG_SLOWNESS(effect(Effect.SLOWNESS,3,400)),
    TURTLE_MASTER(effect(Effect.SLOWNESS,3,400),effect(Effect.RESISTANCE,2,400)),
    LONG_TURTLE_MASTER(effect(Effect.SLOWNESS,3,800),effect(Effect.RESISTANCE,2,800)),
    STRONG_TURTLE_MASTER(effect(Effect.SLOWNESS,5,400),effect(Effect.RESISTANCE,3,400)),
    WATER_BREATHING(effect(Effect.WATER_BREATHING,0,3600)),
    LONG_WATER_BREATHING(effect(Effect.WATER_BREATHING,0,3600)),
    HEALING(effect(Effect.INSTANT_HEALTH,0,0)),
    STRONG_HEALING(effect(Effect.INSTANT_HEALTH,1,0)),
    HARMING(effect(Effect.INSTANT_DAMAGE,0,0)),
    STRONG_HARMING(effect(Effect.INSTANT_DAMAGE,1,0)),
    POISON(effect(Effect.POISON,0,900)),
    LONG_POISON(effect(Effect.POISON,0,1800)),
    STRONG_POISON(effect(Effect.POISON,1,420)),
    REGENERATION(effect(Effect.REGENERATION,0,900)),
    LONG_REGENERATION(effect(Effect.REGENERATION,0,1800)),
    STRONG_REGENERATION(effect(Effect.REGENERATION,1,440)),
    STRENGTH(effect(Effect.STRENGTH,0,3600)),
    LONG_STRENGTH(effect(Effect.STRENGTH,0,9600)),
    STRONG_STRENGTH(effect(Effect.STRENGTH,1,1800)),
    WEAKNESS(effect(Effect.WEAKNESS,0,1800)),
    LONG_WEAKNESS(effect(Effect.WEAKNESS,0,4800)),
    LUCK(effect(Effect.LUCK,0,6000)), // does not exist in Bedrock
    SLOW_FALLING(effect(Effect.SLOW_FALLING,0,1800)),
    LONG_SLOW_FALLING(effect(Effect.SLOW_FALLING,0,4800)),
    WIND_CHARGING(effect(Effect.WIND_CHARGED,0,3600)),
    WEAVING(effect(Effect.WEAVING,0,3600)),
    OOZING(effect(Effect.OOZING,0,3600)),
    INFESTATION(effect(Effect.INFESTED,0,3600));

    public static final Potion[] VALUES = values();

    private final MobEffectInstance[] javaEffects;

    private static MobEffectInstance effect(Effect effect, int amplifier, int duration) {
        return new MobEffectInstance(effect, new MobEffectDetails(amplifier, duration, false, true, true, null));
    }

    Potion(MobEffectInstance... javaEffects) {
        this.javaEffects = javaEffects;
    }

    public MobEffectInstance[] getJavaEffects() {
        return javaEffects;
    }

    public static @Nullable Potion getByJavaId(int javaId) {
        if (javaId >= 0 && javaId < VALUES.length) {
            return VALUES[javaId];
        }
        return null;
    }
}
