const cache = new Map();

export const setCache = (key, value, ttlMs) => {
  cache.set(key, { value, expiry: Date.now() + ttlMs });
};

export const getCache = (key) => {
  const record = cache.get(key);
  if (!record) return null;
  if (Date.now() > record.expiry) {
    cache.delete(key);
    return null;
  }
  return record.value;
};
