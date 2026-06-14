import type { Metadata } from "next";
import { ReactNode } from "react";

import { AuthProvider } from "@/components/auth-provider";
import { ProjectDataProvider } from "@/components/project-data-provider";

import "./globals.css";

export const metadata: Metadata = {
  title: "SecureTask",
  description: "Secure project and document management",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <ProjectDataProvider>{children}</ProjectDataProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
