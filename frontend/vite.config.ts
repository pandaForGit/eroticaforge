import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 默认只监听 127.0.0.1，其它电脑（含 Tailscale 对端）无法用 IP/主机名访问 dev server
    host: true,
    // 通过 MagicDNS（*.ts.net）等非常规 Host 访问时，避免被 Vite 的 DNS 重绑定防护拦截
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
