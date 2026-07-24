export function reactListKey(
  prefix: string,
  id: string | number | null | undefined,
  index: number,
) {
  const normalizedId = id == null || id === "null" || id === "undefined" || id === ""
    ? "missing"
    : String(id)
  return `${prefix}-${normalizedId}-${index}`
}
