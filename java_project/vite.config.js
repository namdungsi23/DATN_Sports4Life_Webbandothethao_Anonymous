import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    include: ['sockjs-client', '@stomp/stompjs'],
  },
  server: {
    port: 5173,
    proxy: {
      '/base': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/base/, ''),
        // Spring Security often redirects with an absolute Location to :8080.
        // The browser would then leave the dev origin and hit CORS. Rewrite so
        // redirects stay under /base and keep same-origin (5173).
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            const loc = proxyRes.headers['location']
            if (!loc || typeof loc !== 'string') return
            const springOrigin = /^(https?:\/\/)(localhost|127\.0\.0\.1):8080/i
            if (!springOrigin.test(loc)) return
            const path = loc.replace(springOrigin, '') || '/'
            proxyRes.headers['location'] = `/base${path.startsWith('/') ? path : `/${path}`}`
          })
        },
      },
    }
  }
})
