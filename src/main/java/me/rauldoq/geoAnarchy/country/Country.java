package me.rauldoq.geoAnarchy.country;

import java.util.Set;
import java.util.UUID;

/**
 * A country, as far as alerts care: who founded it and who its citizens are.
 * @param members every citizen, the founder included
 */
public record Country(String id, String name, UUID founder, Set<UUID> members) {
}
