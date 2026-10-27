/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export for GitHub Pages, so no API routes or force-dynamic pages are allowed.
  output: "export",
  // Served at https://<user>.github.io/relayflow/, not the domain root.
  basePath: "/relayflow",
  trailingSlash: true,
};

export default nextConfig;
