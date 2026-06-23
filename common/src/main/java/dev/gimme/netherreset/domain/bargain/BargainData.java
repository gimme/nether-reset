package dev.gimme.netherreset.domain.bargain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Per-villager record of the bargains struck so far <em>today</em>.
 *
 * <p>A villager racks up unique <em>profession</em> interactions: {@link #professions} holds the distinct employed
 * professions already bargained with, while {@link #wildcardBargains} counts bargains with plain (professionless)
 * villagers, who are jack-of-all-trades and so always count as a fresh, unique interaction. The total
 * {@link #bargains()} is what later feeds restock stock and is what the cap limits.
 *
 * <p>{@link #day} is the game-day ({@code gameTime / 24000}) this record belongs to; a villager mixin compares it
 * against the current day to reset bargains (and decay stock) once per day. The record is immutable — every mutation
 * returns a fresh instance.
 */
public record BargainData(
        Set<Identifier> professions,
        int wildcardBargains,
        long day
) {

    public static final Codec<BargainData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("professions", Set.of()).forGetter(BargainData::professions),
            Codec.INT.optionalFieldOf("wildcardBargains", 0).forGetter(BargainData::wildcardBargains),
            Codec.LONG.optionalFieldOf("day", 0L).forGetter(BargainData::day)
    ).apply(inst, BargainData::new));

    public BargainData {
        professions = Set.copyOf(professions);
    }

    public static BargainData empty() {
        return new BargainData(Set.of(), 0, 0L);
    }

    public static BargainData emptyOn(long day) {
        return new BargainData(Set.of(), 0, day);
    }

    /** Total unique bargains today — distinct employed professions plus plain-villager (wildcard) bargains. */
    public int bargains() {
        return professions.size() + wildcardBargains;
    }

    /** Whether this villager has already bargained with the given employed profession today. */
    public boolean hasBargainedWith(Identifier profession) {
        return professions.contains(profession);
    }

    /** Returns a copy with another employed profession recorded as bargained-with today. */
    public BargainData withProfession(Identifier profession) {
        return new BargainData(
                Stream.concat(professions.stream(), Stream.of(profession)).collect(java.util.stream.Collectors.toUnmodifiableSet()),
                wildcardBargains,
                day);
    }

    /** Returns a copy with one more plain-villager (wildcard) bargain recorded today. */
    public BargainData withWildcard() {
        return new BargainData(professions, wildcardBargains + 1, day);
    }

    /** Returns a copy stamped for the given day with the same recorded bargains. */
    public BargainData onDay(long day) {
        return new BargainData(professions, wildcardBargains, day);
    }
}
