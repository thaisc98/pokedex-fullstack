import { z } from "zod";
import type { CustomAttributes, UpdateCollectedPokemonRequest } from "@/shared/api/types";

/**
 * US04 edit form: values, client-side rules and the mapping to/from the API.
 * The rules mirror openapi.yaml (CustomAttributes); the server stays the source of truth.
 */

const LANG = /^[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*$/;
export const TAG_PATTERN = /^[a-z0-9-]{1,30}$/;
export const MAX_TAGS = 10;

/** Tags are stored trimmed and lowercased (`Tag`). */
export const normalizeTag = (tag: string) => tag.trim().toLowerCase();

const optionalCoordinate = (min: number, max: number, label: string) =>
  z
    .number()
    .min(min, `${label} must be between ${min} and ${max}`)
    .max(max, `${label} must be between ${min} and ${max}`)
    .optional();

export const editSchema = z
  .object({
    localizedNames: z.array(
      z.object({
        lang: z
          .string()
          .trim()
          .min(1, "Enter a language code")
          .max(35, "Language code is too long")
          .regex(LANG, "Use a language code like ja, de or pt-BR"),
        value: z.string().trim().min(1, "Enter the name in this language").max(60, "Names can be at most 60 characters"),
      }),
    ),
    region: z.string().trim().max(60, "Region can be at most 60 characters"),
    habitat: z.string().trim().max(60, "Habitat can be at most 60 characters"),
    latitude: optionalCoordinate(-90, 90, "Latitude"),
    longitude: optionalCoordinate(-180, 180, "Longitude"),
    tags: z
      .array(z.string())
      .max(MAX_TAGS, `At most ${MAX_TAGS} tags`)
      .refine((tags) => tags.every((t) => TAG_PATTERN.test(normalizeTag(t))), {
        message: "Tags may only contain a–z, 0–9 and - (up to 30 characters)",
      }),
  })
  .superRefine((values, ctx) => {
    const seen = new Set<string>();
    values.localizedNames.forEach((name, index) => {
      const key = name.lang.trim().toLowerCase();
      if (!key) return;
      if (seen.has(key)) {
        ctx.addIssue({ code: "custom", path: ["localizedNames", index, "lang"], message: "Only one name per language" });
      }
      seen.add(key);
    });
    const hasLat = values.latitude !== undefined;
    const hasLon = values.longitude !== undefined;
    if (hasLat !== hasLon) {
      ctx.addIssue({
        code: "custom",
        path: [hasLat ? "longitude" : "latitude"],
        message: "Enter both latitude and longitude, or neither",
      });
    }
  });

export type EditValues = {
  localizedNames: { lang: string; value: string }[];
  region: string;
  habitat: string;
  latitude: number | undefined;
  longitude: number | undefined;
  tags: string[];
};

export function fromItem(custom: CustomAttributes): EditValues {
  return {
    localizedNames: custom.localizedNames.map(({ lang, value }) => ({ lang, value })),
    region: custom.geo?.region ?? "",
    habitat: custom.geo?.habitat ?? "",
    latitude: custom.geo?.coordinates?.latitude ?? undefined,
    longitude: custom.geo?.coordinates?.longitude ?? undefined,
    tags: [...custom.tags],
  };
}

export function toRequest(values: EditValues, version: number): UpdateCollectedPokemonRequest {
  const region = values.region.trim();
  const habitat = values.habitat.trim();
  const coordinates =
    values.latitude !== undefined && values.longitude !== undefined
      ? { latitude: values.latitude, longitude: values.longitude }
      : undefined;
  const geo =
    region || habitat || coordinates
      ? { ...(region ? { region } : {}), ...(habitat ? { habitat } : {}), ...(coordinates ? { coordinates } : {}) }
      : null;
  return {
    customAttributes: {
      localizedNames: values.localizedNames.map((n) => ({ lang: n.lang.trim(), value: n.value.trim() })),
      geo,
      tags: [...new Set(values.tags.map(normalizeTag))],
    },
    version,
  };
}

/** API field path (Spring FieldError notation) → form field path, or null for form-level errors. */
export function mapServerField(path: string): string | null {
  const p = path.replace(/^customAttributes\./, "").replace(/\[(\d+)\]/g, ".$1");
  if (p.startsWith("localizedNames")) return p;
  if (p === "geo.region") return "region";
  if (p === "geo.habitat") return "habitat";
  if (p === "geo.coordinates.longitude") return "longitude";
  if (p.startsWith("geo.coordinates")) return "latitude";
  if (p.startsWith("tags")) return "tags";
  return null;
}

/** DOM id for a form field path, used by the error summary links. */
export const fieldId = (path: string) => `edit-${path.replace(/\./g, "-")}`;
