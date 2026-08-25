export default function RootLoading() {
  return (
    <div
      role="status"
      aria-label="Loading"
      className="flex min-h-dvh items-center justify-center"
    >
      <div className="size-6 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
    </div>
  );
}
