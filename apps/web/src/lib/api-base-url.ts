export const BROWSER_API_BASE_URL = "/backend";

export function serverApiBaseUrl(): string {
  return (
    process.env.RELAYFLOW_API_INTERNAL_URL ?? "http://localhost:8080"
  ).replace(/\/$/, "");
}

export function apiBaseUrl(): string {
  return typeof window === "undefined"
    ? serverApiBaseUrl()
    : BROWSER_API_BASE_URL;
}
