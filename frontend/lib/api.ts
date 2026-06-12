export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

const proxyPath =
  process.env.NEXT_PUBLIC_API_PROXY_PATH?.replace(/\/$/, "") ?? "/api/backend";

export async function apiRequest(
  path: string,
  token: string,
  init: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Bearer ${token}`);

  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${proxyPath}/${path.replace(/^\//, "")}`, {
    ...init,
    headers,
    cache: "no-store",
  });

  if (!response.ok) {
    throw new ApiError(response.status, await errorMessage(response));
  }

  return response;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as {
      detail?: string;
      error?: string;
      message?: string;
    };
    return body.detail ?? body.message ?? body.error ?? `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

export function downloadFilename(response: Response, fallback: string): string {
  const disposition = response.headers.get("Content-Disposition");
  const encoded = disposition?.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const quoted = disposition?.match(/filename="([^"]+)"/i)?.[1];
  return decodeURIComponent(encoded ?? quoted ?? fallback);
}
