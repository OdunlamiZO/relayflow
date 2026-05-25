import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#f7f8fa",
        foreground: "#172033",
        muted: "#667085",
        line: "#d9dee7",
        panel: "#ffffff",
        accent: "#2563eb",
      },
    },
  },
  plugins: [],
};

export default config;
