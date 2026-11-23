// A small in-memory stand-in for Cloudflare KV, for the tests (`npm test`). The Worker
// itself never uses it.
import type { SnapshotStore } from './snapshots.ts';

export type MemoryStore = SnapshotStore & { data: Map<string, string | ArrayBuffer>; metadata: Map<string, unknown>; writes: number };

export function memoryStore(): MemoryStore {
  const data = new Map<string, string | ArrayBuffer>();
  const metadata = new Map<string, unknown>();
  const store = {
    data,
    metadata,
    writes: 0,
    async get(key: string, type: 'json' | 'arrayBuffer') {
      const value = data.get(key);
      if (value === undefined) return null;
      return type === 'json' ? JSON.parse(value as string) : value;
    },
    async put(key: string, value: string | ArrayBuffer | ArrayBufferView, options?: { metadata?: unknown }) {
      store.writes++;
      data.set(key, typeof value === 'string' ? value : ArrayBuffer.isView(value) ? (value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength) as ArrayBuffer) : value);
      if (options?.metadata === undefined) metadata.delete(key);
      else metadata.set(key, options.metadata);
    },
    async delete(key: string) {
      data.delete(key);
      metadata.delete(key);
    },
    async list({ prefix }: { prefix: string }) {
      return { keys: [...data.keys()].filter((name) => name.startsWith(prefix)).map((name) => ({ name, metadata: metadata.get(name) })) };
    },
  };
  return store as MemoryStore;
}
