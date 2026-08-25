import { QueryClient, isServer } from "@tanstack/react-query";

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // Avoid an immediate refetch-on-mount right after SSR hydration;
        // individual queries opt into a shorter staleTime as needed.
        staleTime: 60 * 1000,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

/**
 * SSR-safe QueryClient accessor (TanStack Query's documented Next.js App
 * Router pattern): always a fresh client per request on the server, but a
 * single memoized instance in the browser so client-side navigations share
 * the cache instead of refetching everything.
 */
export function getQueryClient() {
  if (isServer) {
    return makeQueryClient();
  }

  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient();
  }
  return browserQueryClient;
}
