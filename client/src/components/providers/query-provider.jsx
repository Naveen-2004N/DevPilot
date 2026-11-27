
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";

const QueryProvider = ({ children }) => {
  const [query] = useState(() => new QueryClient());

  return (
    <QueryClientProvider client={query}>
      {children}
    </QueryClientProvider>
  );
};

export default QueryProvider;
