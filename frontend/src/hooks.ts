import { useCallback, useEffect, useId, useState } from "react";
import { api, errorText } from "./api";
import type { Feedback } from "./types";

export function useResource<T>(path: string) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setData(null);
    api<T>(path, { signal: controller.signal })
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((error) => {
        if (!controller.signal.aborted) setError(errorText(error));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [path, version]);
  return {
    data,
    setData,
    error,
    loading,
    refresh: useCallback(() => setVersion((v) => v + 1), []),
  };
}
export function useDirty(dirty: boolean, feedback: Feedback) {
  const id = useId();
  const { onDirty } = feedback;
  useEffect(() => {
    onDirty(id, dirty);
    return () => onDirty(id, false);
  }, [dirty, id, onDirty]);
}
export function useAction(feedback: Feedback) {
  const id = useId();
  const { onBusy, onNotice } = feedback;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => () => onBusy(id, false), [id, onBusy]);
  async function run(action: () => Promise<void>, success?: string) {
    if (busy) return;
    setBusy(true);
    setError("");
    onBusy(id, true);
    try {
      await action();
      if (success) onNotice(success);
    } catch (error) {
      setError(errorText(error));
    } finally {
      setBusy(false);
      onBusy(id, false);
    }
  }
  return { busy, error, setError, run };
}
