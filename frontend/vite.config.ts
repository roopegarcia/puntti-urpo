import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

const proxy = {
  "/api": { target: process.env.API_PROXY_TARGET ?? "http://127.0.0.1:8080" },
};
export default defineConfig({
  plugins: [react()],
  server: { host: "127.0.0.1", port: 5175, strictPort: true, proxy },
  preview: { host: "127.0.0.1", port: 4173, strictPort: true, proxy },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.ts",
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
