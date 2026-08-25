// Unauthenticated route group (login, password reset, 2FA enrollment —
// F0.2.*). Deliberately no nav shell: centered, minimal chrome.
export default function AuthGroupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-dvh items-center justify-center p-4">
      <div className="w-full max-w-sm">{children}</div>
    </div>
  );
}
