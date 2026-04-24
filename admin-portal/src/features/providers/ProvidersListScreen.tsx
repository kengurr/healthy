import { useState } from 'react';
import { useProviders, useVerificationQueue, useApproveProvider, useRejectProvider } from '../../hooks/useAdmin';

type Tab = 'all' | 'verification';

export function ProvidersListScreen() {
  const [tab, setTab] = useState<Tab>('verification');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [rejectReason, setRejectReason] = useState<string>('');
  const [rejectingId, setRejectingId] = useState<number | null>(null);

  const { data: allProviders, isLoading: loadingAll } = useProviders(statusFilter || undefined);
  const { data: queue, isLoading: loadingQueue } = useVerificationQueue();
  const approveMutation = useApproveProvider();
  const rejectMutation = useRejectProvider();

  const isLoading = tab === 'verification' ? loadingQueue : loadingAll;

  async function handleApprove(id: number) {
    try {
      await approveMutation.mutateAsync(id);
    } catch (e) {
      console.error('Failed to approve provider', e);
    }
  }

  async function handleReject(id: number) {
    try {
      await rejectMutation.mutateAsync({ id, reason: rejectReason || undefined });
      setRejectingId(null);
      setRejectReason('');
    } catch (e) {
      console.error('Failed to reject provider', e);
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Provider Management</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>Verify and manage healthcare provider accounts</p>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem', borderBottom: '1px solid #e2e8f0', paddingBottom: '0.75rem' }}>
        <button
          onClick={() => setTab('verification')}
          style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600, color: tab === 'verification' ? '#1e40af' : '#64748b', borderBottom: tab === 'verification' ? '2px solid #1e40af' : '2px solid transparent', paddingBottom: '0.5rem' }}
        >
          Verification Queue
          {queue && queue.length > 0 && (
            <span style={{ marginLeft: '0.5rem', background: '#ef4444', color: 'white', borderRadius: '9999px', padding: '0.125rem 0.5rem', fontSize: '0.75rem' }}>{queue.length}</span>
          )}
        </button>
        <button
          onClick={() => setTab('all')}
          style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600, color: tab === 'all' ? '#1e40af' : '#64748b', borderBottom: tab === 'all' ? '2px solid #1e40af' : '2px solid transparent', paddingBottom: '0.5rem' }}
        >
          All Providers
        </button>
      </div>

      {/* Filter (only for all tab) */}
      {tab === 'all' && (
        <div style={{ marginBottom: '1rem' }}>
          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }}
          >
            <option value="">All statuses</option>
            <option value="PENDING_VERIFICATION">Pending Verification</option>
            <option value="ACTIVE">Active</option>
            <option value="SUSPENDED">Suspended</option>
          </select>
        </div>
      )}

      {isLoading && <p style={{ color: '#64748b' }}>Loading...</p>}

      {/* Verification Queue */}
      {tab === 'verification' && !loadingQueue && (
        <>
          {queue && queue.length === 0 ? (
            <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#16a34a' }}>
              No providers pending verification.
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Name</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Email</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Profession</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Specialty</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Submitted</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {queue?.map(p => (
                  <tr key={p.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>{p.firstName} {p.lastName}</td>
                    <td style={{ padding: '0.75rem 1rem', color: '#475569' }}>{p.email}</td>
                    <td style={{ padding: '0.75rem 1rem' }}>{p.profession}</td>
                    <td style={{ padding: '0.75rem 1rem' }}>{p.specialty ?? '—'}</td>
                    <td style={{ padding: '0.75rem 1rem', color: '#64748b', fontSize: '0.875rem' }}>{new Date(p.createdAt).toLocaleDateString('sl-SI')}</td>
                    <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                      {rejectingId === p.id ? (
                        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', justifyContent: 'flex-end' }}>
                          <input
                            type="text"
                            placeholder="Reason (optional)"
                            value={rejectReason}
                            onChange={e => setRejectReason(e.target.value)}
                            style={{ padding: '0.375rem 0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontSize: '0.875rem' }}
                          />
                          <button onClick={() => handleReject(p.id)} style={{ padding: '0.375rem 0.75rem', background: '#dc2626', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Confirm</button>
                          <button onClick={() => setRejectingId(null)} style={{ padding: '0.375rem 0.75rem', background: '#e2e8f0', color: '#475569', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Cancel</button>
                        </div>
                      ) : (
                        <>
                          <button
                            onClick={() => handleApprove(p.id)}
                            disabled={approveMutation.isPending}
                            style={{ padding: '0.375rem 0.75rem', background: '#16a34a', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem', marginRight: '0.5rem' }}
                          >
                            {approveMutation.isPending ? 'Approving...' : 'Approve'}
                          </button>
                          <button
                            onClick={() => setRejectingId(p.id)}
                            style={{ padding: '0.375rem 0.75rem', background: 'white', color: '#dc2626', border: '1px solid #dc2626', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}
                          >
                            Reject
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {/* All Providers */}
      {tab === 'all' && !loadingAll && (
        <>
          {allProviders && allProviders.length === 0 ? (
            <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#94a3b8' }}>No providers found.</div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>ID</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Name</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Email</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Role</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                {allProviders?.map(p => (
                  <tr key={p.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '0.75rem 1rem', color: '#94a3b8', fontSize: '0.875rem' }}>#{p.id}</td>
                    <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>{p.firstName} {p.lastName}</td>
                    <td style={{ padding: '0.75rem 1rem', color: '#475569' }}>{p.email}</td>
                    <td style={{ padding: '0.75rem 1rem' }}>{p.role?.name ?? p.role}</td>
                    <td style={{ padding: '0.75rem 1rem' }}>
                      <span style={{ padding: '0.25rem 0.5rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: 600, background: p.status === 'ACTIVE' ? '#dcfce7' : p.status === 'PENDING_VERIFICATION' ? '#fef9c3' : '#f1f5f9', color: p.status === 'ACTIVE' ? '#166534' : p.status === 'PENDING_VERIFICATION' ? '#854d0e' : '#475569' }}>{p.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}