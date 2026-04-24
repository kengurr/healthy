import { useState } from 'react';
import { useBookings, useCancelBooking } from '../../hooks/useAdmin';

export function BookingsListScreen() {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [cancelReason, setCancelReason] = useState<string>('');
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const { data: bookings, isLoading } = useBookings(statusFilter || undefined);
  const cancelMutation = useCancelBooking();

  async function handleCancel(id: number) {
    try {
      await cancelMutation.mutateAsync({ id, reason: cancelReason || undefined });
      setCancellingId(null);
      setCancelReason('');
    } catch (e) {
      console.error('Failed to cancel booking', e);
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Booking Management</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>View and manage all platform bookings</p>

      {/* Filter */}
      <div style={{ marginBottom: '1rem' }}>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }}
        >
          <option value="">All statuses</option>
          <option value="REQUESTED">Requested</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="IN_PROGRESS">In Progress</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {isLoading && <p style={{ color: '#64748b' }}>Loading...</p>}

      {!isLoading && bookings && bookings.length === 0 && (
        <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#94a3b8' }}>
          No bookings found.
        </div>
      )}

      {!isLoading && bookings && bookings.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>ID</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Patient ID</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Provider ID</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Date</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Time</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Status</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Payment</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'right', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {bookings.map(b => (
              <tr key={b.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '0.75rem 1rem', color: '#94a3b8', fontSize: '0.875rem' }}>#{b.id}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569' }}>{b.patientId}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569' }}>{b.providerId ?? '—'}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{b.date}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{b.timeSlot}</td>
                <td style={{ padding: '0.75rem 1rem' }}>
                  <span style={{
                    padding: '0.25rem 0.5rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: b.status === 'CONFIRMED' ? '#dcfce7' : b.status === 'COMPLETED' ? '#dbeafe' : b.status === 'CANCELLED' ? '#fee2e2' : '#fef9c3',
                    color: b.status === 'CONFIRMED' ? '#166534' : b.status === 'COMPLETED' ? '#1e40af' : b.status === 'CANCELLED' ? '#991b1b' : '#854d0e'
                  }}>{b.status}</span>
                </td>
                <td style={{ padding: '0.75rem 1rem' }}>
                  <span style={{
                    padding: '0.25rem 0.5rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: b.paymentStatus === 'PAID' ? '#dcfce7' : b.paymentStatus === 'PENDING' ? '#fef9c3' : b.paymentStatus === 'REFUNDED' ? '#dbeafe' : '#fee2e2',
                    color: b.paymentStatus === 'PAID' ? '#166534' : b.paymentStatus === 'PENDING' ? '#854d0e' : b.paymentStatus === 'REFUNDED' ? '#1e40af' : '#991b1b'
                  }}>{b.paymentStatus}</span>
                </td>
                <td style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>
                  {cancellingId === b.id ? (
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', justifyContent: 'flex-end' }}>
                      <input
                        type="text"
                        placeholder="Reason (optional)"
                        value={cancelReason}
                        onChange={e => setCancelReason(e.target.value)}
                        style={{ padding: '0.375rem 0.5rem', borderRadius: '4px', border: '1px solid #e2e8f0', fontSize: '0.875rem' }}
                      />
                      <button onClick={() => handleCancel(b.id)} style={{ padding: '0.375rem 0.75rem', background: '#dc2626', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Confirm</button>
                      <button onClick={() => setCancellingId(null)} style={{ padding: '0.375rem 0.75rem', background: '#e2e8f0', color: '#475569', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}>Cancel</button>
                    </div>
                  ) : (
                    <button
                      onClick={() => setCancellingId(b.id)}
                      disabled={b.status === 'CANCELLED' || b.status === 'COMPLETED'}
                      style={{ padding: '0.375rem 0.75rem', background: 'white', color: '#dc2626', border: '1px solid #dc2626', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem', opacity: b.status === 'CANCELLED' || b.status === 'COMPLETED' ? 0.5 : 1 }}
                    >
                      Cancel
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
