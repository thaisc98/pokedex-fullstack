import { Label, Meter } from "@heroui/react";
import type { BaseStat } from "../types";

const STAT_LABELS: Readonly<Record<BaseStat["name"], string>> = {
  hp: "HP",
  attack: "Attack",
  defense: "Defense",
  "special-attack": "Sp. Atk",
  "special-defense": "Sp. Def",
  speed: "Speed",
};

const STAT_ORDER = Object.keys(STAT_LABELS) as BaseStat["name"][];
const MAX_STAT = 255;

/** Fill color by band; the number is always shown, so color is never the only signal. */
function fillClass(value: number) {
  if (value >= 100) return "fill-success";
  if (value < 50) return "fill-warning";
  return "fill-accent";
}

/**
 * The six base stats as HeroUI Meters (native meter semantics, 0–255) with a visible
 * label and value.
 *
 * The bar is an SVG `<rect width>` instead of `Meter.Fill`: Meter.Fill sizes itself with an
 * inline `style="width:…"`, which our CSP (style-src without 'unsafe-inline') blocks in
 * server-rendered HTML. SVG attributes aren't styles, so the bar renders on the server.
 */
export function StatMeters({ stats }: { stats: BaseStat[] }) {
  const sorted = [...stats].sort((a, b) => STAT_ORDER.indexOf(a.name) - STAT_ORDER.indexOf(b.name));
  const total = stats.reduce((sum, s) => sum + s.value, 0);
  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-3">
        {sorted.map((stat) => (
          <li key={stat.name}>
            <Meter
              value={stat.value}
              minValue={0}
              maxValue={MAX_STAT}
              valueLabel={String(stat.value)}
              className="w-full"
            >
              <Label className="text-sm text-muted">{STAT_LABELS[stat.name]}</Label>
              <Meter.Output className="text-sm font-semibold tabular-nums" />
              <Meter.Track>
                <svg
                  aria-hidden="true"
                  className="block h-full w-full"
                  viewBox={`0 0 ${MAX_STAT} 1`}
                  preserveAspectRatio="none"
                >
                  <rect x="0" y="0" height="1" width={stat.value} className={fillClass(stat.value)} />
                </svg>
              </Meter.Track>
            </Meter>
          </li>
        ))}
      </ul>
      <p className="flex justify-between border-t border-separator pt-2 text-sm">
        <span className="text-muted">Total</span>
        <span className="font-semibold tabular-nums">{total}</span>
      </p>
    </div>
  );
}
