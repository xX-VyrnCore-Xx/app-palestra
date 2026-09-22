import { Inter } from "next/font/google";
import "./globals.css";
import { ToastProvider } from "../components/Toast";

const inter = Inter({ subsets: ["latin"], variable: "--font-sans", display: "swap" });

export const metadata = {
  title: "Vibe Fitness — Gestionale PT",
  description: "Area riservata Personal Trainer",
  robots: {
    // Not linked from anywhere, but belt-and-suspenders: keep it out of search indexes too.
    index: false,
    follow: false,
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="it" className={inter.variable}>
      <body className="min-h-screen bg-[#0f0b17] text-white antialiased">
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
