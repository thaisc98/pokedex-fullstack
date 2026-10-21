-- COLLECTED_POKEMON (architecture.md §2.7, BR-05..BR-08). One row per Pokédex id: the unique constraint
-- closes the concurrent first-sync race (the adapter maps it to a version conflict and sync retries once).
-- The upstream snapshot is flat columns plus JSONB arrays (read-only, never queried field by field); tags
-- and localized names are our own data and are filtered on, so they get their own tables.
CREATE TABLE collected_pokemon (
    id          UUID          NOT NULL,
    pokedex_id  INTEGER       NOT NULL,
    name        VARCHAR(50)   NOT NULL,
    sprite_url  VARCHAR(2048) NOT NULL,
    category    VARCHAR(100)  NOT NULL,
    weight_hg   INTEGER       NOT NULL,
    abilities   JSONB         NOT NULL,
    base_stats  JSONB         NOT NULL,
    region      VARCHAR(60),
    habitat     VARCHAR(60),
    latitude    NUMERIC,
    longitude   NUMERIC,
    version     BIGINT        NOT NULL DEFAULT 0,
    synced_at   TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_collected_pokemon PRIMARY KEY (id),
    CONSTRAINT uk_collected_pokemon_pokedex_id UNIQUE (pokedex_id),
    CONSTRAINT ck_collected_pokemon_pokedex_id CHECK (pokedex_id > 0),
    CONSTRAINT ck_collected_pokemon_name CHECK (name ~ '^[a-z0-9-]{1,50}$'),
    CONSTRAINT ck_collected_pokemon_weight CHECK (weight_hg >= 0),
    CONSTRAINT ck_collected_pokemon_abilities CHECK (jsonb_typeof(abilities) = 'array'),
    CONSTRAINT ck_collected_pokemon_base_stats CHECK (jsonb_typeof(base_stats) = 'array'),
    CONSTRAINT ck_collected_pokemon_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_collected_pokemon_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_collected_pokemon_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CONSTRAINT ck_collected_pokemon_version CHECK (version >= 0)
);

CREATE TABLE collected_pokemon_tag (
    collected_pokemon_id UUID        NOT NULL,
    tag                  VARCHAR(30) NOT NULL,
    CONSTRAINT pk_collected_pokemon_tag PRIMARY KEY (collected_pokemon_id, tag),
    CONSTRAINT fk_collected_pokemon_tag_pokemon FOREIGN KEY (collected_pokemon_id)
        REFERENCES collected_pokemon (id) ON DELETE CASCADE,
    CONSTRAINT ck_collected_pokemon_tag_format CHECK (tag ~ '^[a-z0-9-]{1,30}$')
);

-- Exact-tag filter (UC-05): find the Pokémon that carry a tag.
CREATE INDEX ix_collected_pokemon_tag_tag ON collected_pokemon_tag (tag);

CREATE TABLE collected_pokemon_localized_name (
    collected_pokemon_id UUID        NOT NULL,
    lang                 VARCHAR(35) NOT NULL,
    value                VARCHAR(60) NOT NULL,
    CONSTRAINT pk_collected_pokemon_localized_name PRIMARY KEY (collected_pokemon_id, lang),
    CONSTRAINT fk_collected_pokemon_localized_name_pokemon FOREIGN KEY (collected_pokemon_id)
        REFERENCES collected_pokemon (id) ON DELETE CASCADE
);
