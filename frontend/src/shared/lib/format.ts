/** Display formatters (docs/ui-ux.md §8): units always shown, one decimal; numbers zero-padded. */

export const formatDexNumber = (id: number) => `#${String(id).padStart(3, "0")}`;

const oneDecimal = new Intl.NumberFormat("en", {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

export const formatKg = (kg: number) => `${oneDecimal.format(kg)} kg`;
export const formatMeters = (m: number) => `${oneDecimal.format(m)} m`;
