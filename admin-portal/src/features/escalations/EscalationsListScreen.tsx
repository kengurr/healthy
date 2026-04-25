import { useState } from 'react';
import { useEscalations, useResolveEscalation } from '../../hooks/useAdmin';
import type { EscalationStatus } from '@zdravdom/types';

const STATUS_OPTIONS: EscalationStatus[] = ['OPEN', 'IN_REVIEW', 'RESOLVED', 'ESCALATED_TO_EMERGENCY_SERVICES'];

const STATUS_COLORS: Record<EscalationStatus, { bg: string; color: string }> = {
  OPEN: { bg: '#fee2e2', color: '#991b1b' },
  IN_REVIEW: { bg: '#fef9c3', color: '#854d0e' },
  RESOLVED: { bg: '#dcfce7', color: '#166534' },
  ESCALATED_TO_EMERGENCY_SERVICES: { bg: '#dc2626', color: '#ffffff' },
};

const URGENCY_COLORS: Record<string, { bg: string; color: string }> = {
  MEDICAL_EMERGENCY: { bg: '#fee2e2', color: '#991b1b' },
  SUSPECTED_ABUSE: { bg: '#fef9c3', color: '#854d0e' },
  MEDICATION_ERROR: { bg: '#fee2e2', color: '#991b1b' },
  PATIENT_DECLINING: { bg: '#fef9c3', color: '#854d0e' },
  OTHER: { bg: '#f1f5f9', color: '#475569' },
};

export function EscalationsListScreen() {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [resolvingId, setResolvingId] = useState<number | null>(null);
  const [resolution, setResolution] = useState('');
  const [actionTaken, setActionTaken] = useState('');
  const [newStatus, setNewStatus] = useState<EscalationStatus>('RESOLVED');
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const { data: escalations, isLoading } = useEscalations(statusFilter || undefined);
  const resolveMutation = useResolveEscalation();

  async function handleResolve(id: number) {
    try {
      await resolveMutation.mutateAsync({ id, data: { status: newStatus, resolution: resolution || undefined, actionTaken: actionTaken || undefined } });
      setResolvingId(null);
      setResolution('');
      setActionTaken('');
      setNewStatus('RESOLVED');
      setMessage({ type: 'success', text: 'Escalation updated.' });
      setTimeout(() => setMessage(null), 3000);
    } catch (e) {
      setMessage({ type: 'error', text: 'Failed to update escalation.' });
      setTimeout(() => setMessage(null), 3000);
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Escalations</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>Review and manage urgent clinical escalations</p>

      {message && (
        <div style={{
          padding: '0.75rem 1rem',
          borderRadius: '6px',
          marginBottom: '1rem',
          background: message.type === 'success' ? '#dcfce7' : '#fee2e2',
          color: message.type === 'success' ? '#166534' : '#991b1b',
          fontSize: '0.875rem',
          fontWeight: 500,
        }}>
          {message.text}
        </div>
      )}

      {/* Filter */}
      <div style={{ marginBottom: '1rem' }}>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }}
        >
          <option value="">All statuses</option>
          {STATUS_OPTIONS.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
        </select>
      </div>

      {isLoading && <p style={{ color: '#64748b' }}>Loading...</p>}

      {!isLoading && escalations && escalations.length === 0 && (
        <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#16a34a' }}>
          No escalations found.
        </div>
      )}

      {!isLoading && escalations && escalations.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>ID</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Urgency</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Provider</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Patient</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Description</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Status</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Created</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'right', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {escalations.map(e => {
              const urgencyStyle = URGENCY_COLORS[e.urgencyType] ?? { bg: '#f1f5f9', color: '#475569' };
              const statusStyle = STATUS_COLORS[e.status] ?? { bg: '#f1f5f9', color: '#475569' };
              return (
                <tr key={e.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '0.75rem 1rem', color: '#94a3b8', fontSize: '0.875rem' }}>#{e.id}</td>
                  <td style={{ padding: '0.75rem 1rem' }}>
                    <span style={{ padding: '0.25rem 0.5rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 600, background: urgencyStyle.bg, color: urgencyStyle.color }}>
                      {e.urgencyType.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{e.providerName ?? '—'}</td>
                  <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{e.patientName ?? '—'}</td>
                  <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{e.description}</td>
                  <td style={{ padding: '0.75rem 1rem' }}>
                    <span style={{ padding: '0.25rem 0.5rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 600, background: statusStyle.bg, color: statusStyle.color }}>
                      {e.status.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td style={{ padding: '0.75rem 1rem', color: '#64748b', fontSize: '0.875rem' }}>{new Date(e.createdAt).toLocaleDateString('sl-SI')}</td>
                  <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                    {resolvingId === e.id ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', alignItems: 'flex-end', minWidth: '280px' }}>
                        <select value={newStatus} onChange={ev => setNewStatus(ev.target.value as EscalationStatus)} style={{ padding: '0.375rem 0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontSize: '0.875rem', width: '100%' }}>
                          {STATUS_OPTIONS.map(s => <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>)}
                        </select>
                        <input type="text" placeholder="Action taken" value={actionTaken} onChange={ev => setActionTaken(ev.target.value)} style={{ padding: '0.375rem 0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontSize: '0.875rem', width: '100%' }} />
                        <input type="text" placeholder="Resolution notes" value={resolution} onChange={ev => setResolution(ev.target.value)} style={{ padding: '0.375rem 0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontSize: '0.875rem', width: '100%' }} />
                        <div style={{ display: 'flex', gap: '0.25rem' }}>
                          <button onClick={() => handleResolve(e.id)} style={{ padding: '0.375rem 0.75rem', background: '#16a34a', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Save</button>
                          <button onClick={() => setResolvingId(null)} style={{ padding: '0.375rem 0.75rem', background: '#e2e8f0', color: '#475569', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Cancel</button>
                        </div>
                      </div>
                    ) : (
                      <button
                        onClick={() => setResolvingId(e.id)}
                        disabled={e.status === 'RESOLVED' || e.status === 'ESCALATED_TO_EMERGENCY_SERVICES'}
                        style={{ padding: '0.375rem 0.75rem', background: '#1e40af', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem', opacity: e.status === 'RESOLVED' || e.status === 'ESCALATED_TO_EMERGENCY_SERVICES' ? 0.5 : 1 }}
                      >
                        Update
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
