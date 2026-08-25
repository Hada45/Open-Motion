/* Open Motion V5.31 PWA service worker.
   Network-first navigation keeps GitHub deployments fresh while retaining an
   offline shell. User-imported Blob/File media is never cached here. */
const CACHE_NAME = 'open-motion-pwa-v531';
const SHELL = [
  './',
  './manifest.webmanifest',
  './icons/open-motion-192.png',
  './icons/open-motion-512.png',
  './icons/open-motion-maskable-512.png',
  './icons/apple-touch-icon.png',
  './icons/favicon-64.png'
];

self.addEventListener('install', event => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(SHELL))
      .catch(error => console.warn('[Open Motion PWA] shell pre-cache skipped:', error))
  );
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names.filter(name => name.startsWith('open-motion-pwa-') && name !== CACHE_NAME)
      .map(name => caches.delete(name)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (request.mode === 'navigate') {
    event.respondWith((async () => {
      try {
        const fresh = await fetch(request);
        if (fresh && fresh.ok) {
          const cache = await caches.open(CACHE_NAME);
          cache.put('./', fresh.clone()).catch(() => {});
        }
        return fresh;
      } catch (_) {
        return (await caches.match(request)) ||
               (await caches.match('./')) ||
               Response.error();
      }
    })());
    return;
  }

  const isPwaAsset =
    url.pathname.endsWith('/manifest.webmanifest') ||
    url.pathname.includes('/icons/');
  if (isPwaAsset) {
    event.respondWith((async () => {
      const cached = await caches.match(request);
      if (cached) {
        fetch(request).then(async fresh => {
          if (fresh && fresh.ok) {
            const cache = await caches.open(CACHE_NAME);
            cache.put(request, fresh);
          }
        }).catch(() => {});
        return cached;
      }
      const fresh = await fetch(request);
      if (fresh && fresh.ok) {
        const cache = await caches.open(CACHE_NAME);
        cache.put(request, fresh.clone());
      }
      return fresh;
    })());
  }
});
