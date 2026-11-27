
import { getApiBaseUrl, ApiError } from "@/lib/api";

export async function streamChatMessage(
  sessionId,
  content,
  handlers = {}
) {
  const res = await fetch(
    `${getApiBaseUrl()}/api/chat/sessions/${sessionId}/messages`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ content }),
      signal: handlers.signal,
    }
  );

  if (!res.ok) {
    let message = res.statusText;

    try {
      const data = await res.json();
      message = data.message ?? data.error ?? message;
    } catch {
      // Ignore JSON parsing errors.
    }

    throw new ApiError(res.status, message);
  }

  if (!res.body) {
    throw new Error("No response body for SSE stream");
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();

  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();

    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // Split events on double newlines (both CRLF and LF)
    const parts = buffer.split(/\r?\n\r?\n/);
    buffer = parts.pop() ?? "";

    for (const part of parts) {
      if (!part.trim()) continue;

      const lines = part.split(/\r?\n/);

      let event = "message";
      const dataLines = [];

      for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.startsWith("event:")) {
          event = trimmed.slice(6).trim();
        } else if (trimmed.startsWith("data:")) {
          dataLines.push(trimmed.slice(5).trimStart());
        }
      }

      const data = dataLines.join("\n");

      if (!data) continue;

      try {
        if (event === "token") {
          let text = data;
          try {
            const parsed = JSON.parse(data);
            if (typeof parsed === "string") {
              text = parsed;
            } else if (parsed && typeof parsed === "object") {
              text = parsed.content ?? parsed.token ?? parsed.text ?? "";
            } else if (typeof parsed === "number" || typeof parsed === "boolean") {
              text = String(parsed);
            }
          } catch {
            text = data;
          }
          if (text) {
            handlers.onToken?.(text);
          }
        } else if (event === "user_message") {
          handlers.onUserMessage?.(JSON.parse(data));
        } else if (event === "assistant_message") {
          handlers.onAssistantMessage?.(JSON.parse(data));
        } else if (event === "done") {
          handlers.onDone?.();
        }
      } catch (err) {
        handlers.onError?.(
          err instanceof Error
            ? err
            : new Error("Failed to parse SSE event")
        );
      }
    }
  }

  handlers.onDone?.();
}
