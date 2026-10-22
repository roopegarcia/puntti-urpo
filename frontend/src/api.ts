let csrf: { token: string; headerName: string } | null = null;
export function setCsrf(value: typeof csrf) {
  csrf = value;
}

interface Problem {
  detail?: string;
  errors?: { field?: string; message?: string }[];
}
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/** No automatic mutation retries: a lost response does not prove the write failed. */
export async function api<T>(
  path: string,
  options: {
    method?: string;
    body?: unknown;
    form?: URLSearchParams;
    signal?: AbortSignal;
  } = {},
): Promise<T> {
  const controller = new AbortController();
  const abort = () => controller.abort();
  options.signal?.addEventListener("abort", abort, { once: true });
  if (options.signal?.aborted) controller.abort();
  const timeout = window.setTimeout(abort, 15000);
  try {
    const response = await fetch(path, {
      method: options.method ?? "GET",
      signal: controller.signal,
      headers: {
        Accept: "application/json",
        ...(csrf && options.method && options.method !== "GET"
          ? { [csrf.headerName]: csrf.token }
          : {}),
        ...(options.form
          ? { "Content-Type": "application/x-www-form-urlencoded" }
          : {}),
        ...(options.body === undefined
          ? {}
          : { "Content-Type": "application/json" }),
      },
      body:
        options.form ??
        (options.body === undefined ? undefined : JSON.stringify(options.body)),
    });
    if (response.status === 204) return undefined as T;
    const body: unknown = await response.json().catch(() => null);
    if (!response.ok) {
      if (response.status === 401 && !path.startsWith("/api/auth/")) {
        setCsrf(null);
        window.dispatchEvent(new Event("session-expired"));
      }
      if (response.status === 403 && !path.startsWith("/api/auth/")) {
        try {
          const identity = await fetch("/api/auth/me", {
            signal: controller.signal,
          });
          if (identity.status === 401) {
            setCsrf(null);
            window.dispatchEvent(new Event("session-expired"));
          }
        } catch {
          /* Keep the original mutation error; never retry the write. */
        }
      }
      const problem = body && typeof body === "object" ? (body as Problem) : {};
      const fields = Array.isArray(problem.errors)
        ? problem.errors
            .map((e) => e.message)
            .filter((m) => typeof m === "string")
        : [];
      throw new ApiError(
        response.status,
        fields.length
          ? fields.join(" ")
          : typeof problem.detail === "string"
            ? problem.detail
            : "Tietoja ei voitu käsitellä. Yritä uudelleen.",
      );
    }
    if (body === null)
      throw new ApiError(
        response.status,
        "Palvelimen vastausta ei voitu lukea. Päivitä tiedot ennen uutta tallennusta.",
      );
    return body as T;
  } catch (error) {
    if (options.signal?.aborted)
      throw new DOMException("Request cancelled", "AbortError");
    if (error instanceof ApiError) throw error;
    throw new ApiError(
      0,
      "Yhteys palvelimeen katkesi. Muutoksesi ovat yhä lomakkeessa. Tarkista päivittämällä tiedot ennen tallennuksen uusimista.",
    );
  } finally {
    window.clearTimeout(timeout);
    options.signal?.removeEventListener("abort", abort);
  }
}
export function errorText(error: unknown) {
  return error instanceof Error
    ? error.message
    : "Jokin meni pieleen. Yritä uudelleen.";
}
