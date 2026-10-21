import path from "node:path";
import type { NextConfig } from "next";

/**
 * Where the Next.js server reaches the Spring API. Server side only (never NEXT_PUBLIC_).
 * NOTE: with `output: "standalone"`, `rewrites()` is evaluated at BUILD time and baked into
 * the routes manifest, so the Docker build must pass BACKEND_INTERNAL_URL as a build arg.
 * `src/server/catalogClient.ts` reads the same variable at RUNTIME.
 */
const backendUrl = (process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080").replace(/\/+$/, "");

const nextConfig: NextConfig = {
  // Self-contained server bundle for the Node 26 Alpine image (P6).
  output: "standalone",
  // security.md §5: no `X-Powered-By: Next.js` fingerprint.
  poweredByHeader: false,
  reactStrictMode: true,
  // Pin the project root to this folder so Next.js does not infer the repo root
  // (or a stray lockfile higher up) as the workspace root.
  outputFileTracingRoot: path.join(__dirname),
  turbopack: {
    root: path.join(__dirname),
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // Same origin for the browser: /api/* is forwarded to Spring (architecture.md §3.1, no CORS).
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
  images: {
    // security.md §5: only the PokeAPI sprites repo, so the optimizer can't fetch arbitrary URLs.
    remotePatterns: [
      {
        protocol: "https",
        hostname: "raw.githubusercontent.com",
        port: "",
        pathname: "/PokeAPI/sprites/**",
        search: "",
      },
    ],
  },
};

export default nextConfig;
