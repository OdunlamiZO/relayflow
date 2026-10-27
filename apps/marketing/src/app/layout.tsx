import type { Metadata, Viewport } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import localFont from "next/font/local";

import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
});

const jetBrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-jetbrains-mono",
});

const materialSymbolsRounded = localFont({
  src: "../fonts/material-symbols-rounded.ttf",
  variable: "--font-material-symbols-rounded",
  display: "swap",
});

export const metadata: Metadata = {
  title: "RelayFlow — Customer messaging with workflow automation",
  description: "Customer messaging with developer-grade workflow automation",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

type Props = {
  children: React.ReactNode;
};

export default function RootLayout({ children }: Props) {
  return (
    <html lang="en">
      <body
        className={`${inter.variable} ${jetBrainsMono.variable} ${materialSymbolsRounded.variable}`}
      >
        {children}
      </body>
    </html>
  );
}
