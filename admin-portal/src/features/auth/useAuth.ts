import { useMutation, useQueryClient } from '@tanstack/react-query';
import { clearAuthTokens, getAccessToken } from '@zdravdom/api-client';
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

export function useAuth() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const isAuthenticated = !!getAccessToken();

  const logoutMutation = useMutation({
    mutationFn: async () => {
      // Could call a backend logout endpoint here if it exists
      clearAuthTokens();
    },
    onSuccess: () => {
      queryClient.clear();
      navigate('/login', { replace: true });
    },
  });

  const logout = useCallback(() => {
    logoutMutation.mutate();
  }, [logoutMutation]);

  return {
    isAuthenticated,
    logout,
    isLoggingOut: logoutMutation.isPending,
  };
}