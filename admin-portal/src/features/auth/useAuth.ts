import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { clearAuthTokens, getAccessToken, getUserRole } from '@zdravdom/api-client';
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

function useRoleQuery() {
  return useQuery({
    queryKey: ['auth', 'role'],
    queryFn: () => getUserRole(),
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
}

export function useAuth() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const { data: role } = useRoleQuery();
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
    role,
    logout,
    isLoggingOut: logoutMutation.isPending,
  };
}