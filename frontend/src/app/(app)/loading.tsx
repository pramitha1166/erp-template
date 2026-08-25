export default function AppLoading() {
  return (
    <div
      role="status"
      aria-label="Loading"
      className="flex items-center justify-center py-16"
    >
      <div className="size-6 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
    </div>
  );
}
