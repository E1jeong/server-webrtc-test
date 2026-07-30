import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "UBio Operator Console",
  description: "Face Pro signaling 상태와 통화 요청을 검증하는 관리자 콘솔",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
