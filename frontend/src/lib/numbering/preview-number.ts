import type { NumberingResetPolicy } from "@/lib/api/numbering-api";

/**
 * F0.5.2: a client-side mirror of the backend's `SeriesNumberFormatter`
 * (NUM-1/NUM-3), so the series form can preview the next document number as
 * the template is edited without a round trip. It renders what today's
 * number would look like for the given template and counter — it does not
 * know the series' `currentPeriodKey` (not part of `SeriesView`), so it
 * can't predict a pending fiscal-year rollover the way the backend's actual
 * allocation does; it only reflects the reset policy's date-part rendering.
 */
export function resolvePrefixPreview(template: string, onDate: Date, fiscalYearStartMonth: number): string {
  const yyyy = String(onDate.getFullYear());
  const yy = yyyy.slice(2);
  const mm = String(onDate.getMonth() + 1).padStart(2, "0");
  const fyStartYear = onDate.getMonth() + 1 >= fiscalYearStartMonth ? onDate.getFullYear() : onDate.getFullYear() - 1;
  const fy =
    fiscalYearStartMonth === 1 ? yyyy : `${fyStartYear}-${String((fyStartYear + 1) % 100).padStart(2, "0")}`;
  return template.replaceAll("{YYYY}", yyyy).replaceAll("{YY}", yy).replaceAll("{MM}", mm).replaceAll("{FY}", fy);
}

export function previewNextNumber(
  prefix: string,
  counterWidth: number,
  resetPolicy: NumberingResetPolicy,
  fiscalYearStartMonth: number,
  nextCounter: number,
  onDate: Date = new Date(),
): string {
  const effectiveFiscalYearStartMonth = resetPolicy === "ANNUAL" ? fiscalYearStartMonth : 1;
  const resolvedPrefix = resolvePrefixPreview(prefix, onDate, effectiveFiscalYearStartMonth);
  return resolvedPrefix + String(Math.max(nextCounter, 0)).padStart(counterWidth, "0");
}
