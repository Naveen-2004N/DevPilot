
import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import {
  useCallback,
  useRef,
  useState,
} from "react";

import { api } from "@/lib/api";
import { queryKeys } from "@/lib/query-keys";
import { streamChatMessage } from "@/lib/stream-chat";
import { toast } from "@/components/ui/toast";

export function useChatSessions(repositoryId, enabled = true) {
  return useQuery({
    queryKey: queryKeys.chat.sessions(repositoryId),
    queryFn: () => api.listSessions(repositoryId),
    enabled: Boolean(repositoryId) && enabled,
  });
}

export function useChatMessages(sessionId) {
  return useQuery({
    queryKey: queryKeys.chat.messages(sessionId ?? ""),
    queryFn: () => api.getMessages(sessionId),
    enabled: Boolean(sessionId),
  });
}

export function useCreateChatSession(repositoryId) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (title) => api.createSession(repositoryId, title),

    onSuccess: (session) => {
      void queryClient.invalidateQueries({
        queryKey: queryKeys.chat.sessions(repositoryId),
      });

      queryClient.setQueryData(
        queryKeys.chat.messages(session.id),
        []
      );
    },

    onError: (error) => {
      toast.add({
        title: "Could not create chat",
        description: error.message,
        type: "error",
      });
    },
  });
}

export function useStreamChat(sessionId) {
  const queryClient = useQueryClient();

  const [streaming, setStreaming] = useState(false);
  const [streamText, setStreamText] = useState("");

  const abortRef = useRef(null);
  const rawTextRef = useRef("");
  const displayedLengthRef = useRef(0);
  const isNetworkActiveRef = useRef(false);
  const pendingAssistantMessageRef = useRef(null);
  const animFrameRef = useRef(null);

  // Stop any running animation
  const stopAnimation = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current);
      animFrameRef.current = null;
    }
  }, []);

  // Smooth typewriter tick loop
  const startTypewriter = useCallback(() => {
    stopAnimation();

    const tick = () => {
      const targetLen = rawTextRef.current.length;
      const currentLen = displayedLengthRef.current;
      const remaining = targetLen - currentLen;

      if (remaining > 0) {
        // Dynamic, natural typing pace based on character backlog:
        // Small backlog: 1-2 chars/frame (~60-120 chars/sec) for a natural typing feel
        // Larger backlog: 3-6 chars/frame to stay snappy and not delay the reader
        let step = 1;
        if (remaining > 250) step = 8;
        else if (remaining > 120) step = 5;
        else if (remaining > 50) step = 3;
        else if (remaining > 15) step = 2;

        const nextLen = Math.min(targetLen, currentLen + step);
        displayedLengthRef.current = nextLen;
        setStreamText(rawTextRef.current.slice(0, nextLen));
      }

      // Check if finished typing and network has ended
      if (
        displayedLengthRef.current >= rawTextRef.current.length &&
        !isNetworkActiveRef.current
      ) {
        if (pendingAssistantMessageRef.current) {
          const finalMsg = pendingAssistantMessageRef.current;
          pendingAssistantMessageRef.current = null;

          queryClient.setQueryData(
            queryKeys.chat.messages(sessionId),
            (prev) => [...(prev ?? []), finalMsg]
          );

          setStreamText("");
          setStreaming(false);
        }
        stopAnimation();
        return;
      }

      // Continue animation if network active or text remains to be typed
      if (isNetworkActiveRef.current || displayedLengthRef.current < rawTextRef.current.length) {
        animFrameRef.current = requestAnimationFrame(tick);
      }
    };

    animFrameRef.current = requestAnimationFrame(tick);
  }, [sessionId, queryClient, stopAnimation]);

  const stop = useCallback(() => {
    abortRef.current?.abort();
    isNetworkActiveRef.current = false;
    stopAnimation();

    if (pendingAssistantMessageRef.current) {
      const msg = pendingAssistantMessageRef.current;
      pendingAssistantMessageRef.current = null;
      queryClient.setQueryData(
        queryKeys.chat.messages(sessionId),
        (prev) => [...(prev ?? []), msg]
      );
    }

    setStreaming(false);
    setStreamText("");
  }, [sessionId, queryClient, stopAnimation]);

  const send = useCallback(
    async (content) => {
      if (!sessionId || !content.trim() || streaming) return;

      stop();

      const controller = new AbortController();
      abortRef.current = controller;

      const optimisticId = `temp-${Date.now()}`;

      const optimistic = {
        id: optimisticId,
        role: "USER",
        content: content.trim(),
        citations: [],
        createdAt: new Date().toISOString(),
      };

      queryClient.setQueryData(
        queryKeys.chat.messages(sessionId),
        (prev) => [...(prev ?? []), optimistic]
      );

      rawTextRef.current = "";
      displayedLengthRef.current = 0;
      isNetworkActiveRef.current = true;
      pendingAssistantMessageRef.current = null;

      setStreaming(true);
      setStreamText("");

      startTypewriter();

      try {
        await streamChatMessage(sessionId, content.trim(), {
          signal: controller.signal,

          onUserMessage: (message) => {
            queryClient.setQueryData(
              queryKeys.chat.messages(sessionId),
              (prev) => [
                ...(prev ?? []).filter(
                  (m) => m.id !== optimisticId
                ),
                message,
              ]
            );
          },

          onToken: (token) => {
            if (token == null) return;
            let text = "";
            if (typeof token === "string") {
              text = token;
            } else if (typeof token === "object") {
              text = token.content || token.token || token.text || "";
            } else {
              text = String(token);
            }
            if (text) {
              rawTextRef.current += text;
            }
          },

          onAssistantMessage: (message) => {
            pendingAssistantMessageRef.current = message;
            isNetworkActiveRef.current = false;
          },

          onDone: () => {
            isNetworkActiveRef.current = false;
          },
        });
      } catch (err) {
        if (err.name === "AbortError") return;

        toast.add({
          title: "Message failed",
          description:
            err instanceof Error
              ? err.message
              : "Unknown error",
          type: "error",
        });

        queryClient.setQueryData(
          queryKeys.chat.messages(sessionId),
          (prev) =>
            (prev ?? []).filter(
              (m) => m.id !== optimisticId
            )
        );

        stop();
      }
    },
    [sessionId, streaming, queryClient, startTypewriter, stop]
  );

  return {
    send,
    stop,
    streaming,
    streamText,
  };
}
