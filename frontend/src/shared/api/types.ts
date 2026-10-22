import type { components } from "./schema";

type Schemas = components["schemas"];

export type UserDto = Schemas["UserDto"];
export type Role = Schemas["Role"];
export type LoginRequest = Schemas["LoginRequest"];
export type LoginResponse = Schemas["LoginResponse"];
export type RegisterRequest = Schemas["RegisterRequest"];
export type CsrfTokenDto = Schemas["CsrfTokenDto"];
export type CollectedPokemon = Schemas["CollectedPokemonDto"];
export type CustomAttributes = Schemas["CustomAttributes"];
export type UpdateCollectedPokemonRequest = Schemas["UpdateCollectedPokemonRequest"];
export type CollectionPage = Omit<Schemas["PageResponse"], "content"> & { content: CollectedPokemon[] };
export type FieldViolation = Schemas["FieldViolation"];
