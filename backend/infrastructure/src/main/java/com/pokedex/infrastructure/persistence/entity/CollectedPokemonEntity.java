package com.pokedex.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.jspecify.annotations.Nullable;

/**
 * {@code collected_pokemon} (architecture.md §2.7). The upstream snapshot is flat columns plus two JSONB
 * arrays; tags and localized names are lazy element collections, loaded with an entity graph. Never bound
 * to JSON and never returned from infrastructure.
 */
@Entity
@Table(name = "collected_pokemon")
public class CollectedPokemonEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pokedex_id", nullable = false, updatable = false)
    private int pokedexId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "sprite_url", nullable = false, length = 2048)
    private String spriteUrl;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "weight_hg", nullable = false)
    private int weightHg;

    @Column(name = "abilities", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String abilitiesJson;

    @Column(name = "base_stats", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String baseStatsJson;

    @Column(name = "region", length = 60)
    private @Nullable String region;

    @Column(name = "habitat", length = 60)
    private @Nullable String habitat;

    @Column(name = "latitude")
    private @Nullable BigDecimal latitude;

    @Column(name = "longitude")
    private @Nullable BigDecimal longitude;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "collected_pokemon_tag",
            joinColumns = @JoinColumn(name = "collected_pokemon_id", nullable = false))
    @Column(name = "tag", nullable = false, length = 30)
    private Set<String> tags = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "collected_pokemon_localized_name",
            joinColumns = @JoinColumn(name = "collected_pokemon_id", nullable = false))
    private Set<LocalizedNameEmbeddable> localizedNames = new LinkedHashSet<>();

    /** {@code null} until first persisted, so Spring Data persists instead of merging. */
    @Version
    @Column(name = "version", nullable = false)
    private @Nullable Long version;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** For JPA only. */
    protected CollectedPokemonEntity() {
        this.id = new UUID(0, 0);
        this.name = "";
        this.spriteUrl = "";
        this.category = "";
        this.abilitiesJson = "[]";
        this.baseStatsJson = "[]";
        this.syncedAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    public CollectedPokemonEntity(UUID id, int pokedexId) {
        this();
        this.id = id;
        this.pokedexId = pokedexId;
    }

    public UUID getId() {
        return id;
    }

    public int getPokedexId() {
        return pokedexId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpriteUrl() {
        return spriteUrl;
    }

    public void setSpriteUrl(String spriteUrl) {
        this.spriteUrl = spriteUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getWeightHg() {
        return weightHg;
    }

    public void setWeightHg(int weightHg) {
        this.weightHg = weightHg;
    }

    public String getAbilitiesJson() {
        return abilitiesJson;
    }

    public void setAbilitiesJson(String abilitiesJson) {
        this.abilitiesJson = abilitiesJson;
    }

    public String getBaseStatsJson() {
        return baseStatsJson;
    }

    public void setBaseStatsJson(String baseStatsJson) {
        this.baseStatsJson = baseStatsJson;
    }

    public @Nullable String getRegion() {
        return region;
    }

    public void setRegion(@Nullable String region) {
        this.region = region;
    }

    public @Nullable String getHabitat() {
        return habitat;
    }

    public void setHabitat(@Nullable String habitat) {
        this.habitat = habitat;
    }

    public @Nullable BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(@Nullable BigDecimal latitude) {
        this.latitude = latitude;
    }

    public @Nullable BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(@Nullable BigDecimal longitude) {
        this.longitude = longitude;
    }

    /** The managed set; replace its contents rather than the set so Hibernate tracks the change. */
    public Set<String> getTags() {
        return tags;
    }

    /** The managed set; replace its contents rather than the set so Hibernate tracks the change. */
    public Set<LocalizedNameEmbeddable> getLocalizedNames() {
        return localizedNames;
    }

    public @Nullable Long getVersion() {
        return version;
    }

    public void setVersion(@Nullable Long version) {
        this.version = version;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
