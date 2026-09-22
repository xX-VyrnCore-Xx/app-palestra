/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // The PT app is client-rendered SPA-style (auth/session live in the browser via
  // supabase-js); nothing here needs server components or SSR data fetching.
  eslint: {
    ignoreDuringBuilds: true,
  },
};

export default nextConfig;
