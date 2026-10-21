import path from "node:path";
import type { NextConfig } from "next";

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
};

export default nextConfig;
