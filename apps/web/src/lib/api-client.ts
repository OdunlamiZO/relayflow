// Shared across API clients so `error instanceof ApiError` works everywhere.

export type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
};

export class ApiError extends Error {
  readonly status: number;
  readonly details: unknown;

  constructor(status: number, message: string, details: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export async function readResponseBody(response: Response): Promise<unknown> {
  const contentType = response.headers.get("content-type") ?? "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

export function apiErrorMessage(status: number, details: unknown): string {
  if (
    details &&
    typeof details === "object" &&
    "message" in details &&
    typeof details.message === "string"
  ) {
    return details.message;
  }

  return `Request failed with status ${status}`;
}
