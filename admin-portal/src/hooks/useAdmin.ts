import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient, { adminApi } from '@zdravdom/api-client';
import type { Service } from '@zdravdom/types';

// ─── Dashboard ─────────────────────────────────────────────────────────────────

export function useDashboard() {
  return useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () => adminApi.getDashboard().then(r => r.data),
  });
}

// ─── Providers ─────────────────────────────────────────────────────────────────

export function useProviders(status?: string) {
  return useQuery({
    queryKey: ['admin', 'providers', status],
    queryFn: () => adminApi.listProviders(status).then(r => r.data),
  });
}

export function useVerificationQueue() {
  return useQuery({
    queryKey: ['admin', 'verification-queue'],
    queryFn: () => adminApi.getVerificationQueue().then(r => r.data),
  });
}

export function useApproveProvider() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.approveProvider(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'providers'] });
      qc.invalidateQueries({ queryKey: ['admin', 'verification-queue'] });
    },
  });
}

export function useRejectProvider() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      adminApi.rejectProvider(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'providers'] });
      qc.invalidateQueries({ queryKey: ['admin', 'verification-queue'] });
    },
  });
}

// ─── Bookings ───────────────────────────────────────────────────────────────────

export function useBookings(status?: string) {
  return useQuery({
    queryKey: ['admin', 'bookings', status],
    queryFn: () => adminApi.listBookings(status).then(r => r.data),
  });
}

export function useBooking(id: number) {
  return useQuery({
    queryKey: ['admin', 'bookings', id],
    queryFn: () => adminApi.getBooking(id).then(r => r.data),
    enabled: !!id,
  });
}

export function useCancelBooking() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) =>
      adminApi.cancelBooking(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'bookings'] });
    },
  });
}

export function useAssignProvider() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, providerId, reason }: { id: number; providerId: number; reason?: string }) =>
      adminApi.assignProvider(id, providerId, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'bookings'] });
    },
  });
}

// ─── Services ──────────────────────────────────────────────────────────────────

export function useServices() {
  return useQuery({
    queryKey: ['services'],
    queryFn: () =>
      apiClient.get<{ content: Service[] }>('/services').then(r => r.data.content ?? []),
  });
}

// ─── Escalations ────────────────────────────────────────────────────────────────

export function useEscalations(status?: string) {
  return useQuery({
    queryKey: ['admin', 'escalations', status],
    queryFn: () => adminApi.listEscalations(status).then(r => r.data),
  });
}

export function useResolveEscalation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof adminApi.updateEscalationStatus>[1] }) =>
      adminApi.updateEscalationStatus(id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'escalations'] });
    },
  });
}

// ─── Users ─────────────────────────────────────────────────────────────────────

export function useUsers() {
  return useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => adminApi.listUsers().then(r => r.data),
  });
}