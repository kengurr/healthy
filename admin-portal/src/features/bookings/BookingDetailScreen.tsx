import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBooking, useCancelBooking, useAssignProvider } from '../../hooks/useAdmin';
import type { BookingStatus } from '@zdravdom/types';

const STATUS_COLORS: Record<BookingStatus, { bg: string; color: string }> = {
  REQUESTED: { bg: '#fef9c3', color: '#854d0e' },
  CONFIRMED: { bg: '#dcfce7', color: '#166534' },
  IN_PROGRESS: { bg: '#dbeafe', color: '#1e40af' },
  COMPLETED: { bg: '#d1fae5', color: '#065f46' },
  CANCELLED: { bg: '#fee2e2', color: '#991b1b' },
};

export function BookingDetailScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const bookingId = Number(id);
  const { data: booking, isLoading, isError } = useBooking(bookingId);
  const cancelMutation = useCancelBooking();
  const assignMutation = useAssignProvider();

  const [showAssign, setShowAssign] = useState(false);
  const [showCancel, setShowCancel] = useState(false);
  const [providerId, setProviderId] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [assignReason, setAssignReason] = useState('');
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  async function handleAssign() {
    if (!providerId) return;
    try {
      await assignMutation.mutateAsync({ id: bookingId, providerId: Number(providerId), reason: assignReason || undefined });
      setShowAssign(false);
      setProviderId('');
      setAssignReason('');
      setMessage({ type: 'success', text: 'Provider assigned.' });
      setTimeout(() => setMessage(null), 3000);
    } catch {
      setMessage({ type: 'error', text: 'Failed to assign provider.' });
      setTimeout(() => setMessage(null), 3000);
    }
  }

  async function handleCancel() {
    try {
      await cancelMutation.mutateAsync({ id: bookingId, reason: cancelReason || undefined });
      setShowCancel(false);
      setCancelReason('');
      setMessage({ type: 'success', text: 'Booking cancelled.' });
      setTimeout(() => setMessage(null), 3000);
    } catch {
      setMessage({ type: 'error', text: 'Failed to cancel booking.' });
      setTimeout(() => setMessage(null), 3000);
    }
  }

  if (isLoading) return <div style={{ padding: '2rem' }}>Loading...</div>;
  if (isError || !booking) return <div style={{ padding: '2rem', color: '#dc2626' }}>Booking not found.</div>;

  const statusStyle = STATUS_COLORS[booking.status] ?? { bg: '#f1f5f9', color: '#475569' };

  return (
    <div style={{ padding: '2rem', maxWidth: '800px' }}>
      <button onClick={() => navigate('/bookings')} style={{ marginBottom: '1rem', background: 'none', border: 'none', cursor: 'pointer', color: '#64748b', fontSize: '0.875rem' }}>
        ← Back to Bookings
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700 }}>Booking #{booking.id}</h1>
          <span style={{ padding: '0.25rem 0.75rem', borderRadius: '9999px', fontSize: '0.875rem', fontWeight: 600, background: statusStyle.bg, color: statusStyle.color }}>
            {booking.status}
          </span>
        </div>
        {booking.status !== 'CANCELLED' && booking.status !== 'COMPLETED' && (
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {showAssign ? (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <input type="number" placeholder="Provider ID" value={providerId} onChange={e => setProviderId(e.target.value)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0', width: '100px' }} />
                <input type="text" placeholder="Reason (opt)" value={assignReason} onChange={e => setAssignReason(e.target.value)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0', width: '120px' }} />
                <button onClick={handleAssign} disabled={assignMutation.isPending} style={{ padding: '0.5rem 1rem', background: '#16a34a', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>Confirm</button>
                <button onClick={() => setShowAssign(false)} style={{ padding: '0.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>Cancel</button>
              </div>
            ) : (
              <button onClick={() => setShowAssign(true)} style={{ padding: '0.5rem 1rem', background: '#1e40af', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem' }}>
                Assign Provider
              </button>
            )}
            {showCancel ? (
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <input type="text" placeholder="Reason (optional)" value={cancelReason} onChange={e => setCancelReason(e.target.value)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0', width: '140px' }} />
                <button onClick={handleCancel} disabled={cancelMutation.isPending} style={{ padding: '0.5rem 1rem', background: '#dc2626', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>Confirm</button>
                <button onClick={() => setShowCancel(false)} style={{ padding: '0.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: '6px', cursor: 'pointer' }}>Cancel</button>
              </div>
            ) : (
              <button onClick={() => setShowCancel(true)} style={{ padding: '0.5rem 1rem', background: 'white', color: '#dc2626', border: '1px solid #dc2626', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem' }}>
                Cancel
              </button>
            )}
          </div>
        )}
      </div>

      {message && (
        <div style={{ padding: '0.75rem 1rem', borderRadius: '6px', marginBottom: '1rem', background: message.type === 'success' ? '#dcfce7' : '#fee2e2', color: message.type === 'success' ? '#166534' : '#991b1b', fontSize: '0.875rem', fontWeight: 500 }}>
          {message.text}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
        <InfoCard label="Patient ID" value={`#${booking.patientId}`} />
        <InfoCard label="Provider ID" value={booking.providerId ? `#${booking.providerId}` : '—'} />
        <InfoCard label="Service ID" value={`#${booking.serviceId}`} />
        <InfoCard label="Date" value={booking.date} />
        <InfoCard label="Time" value={booking.timeSlot} />
        <InfoCard label="Payment" value={`€${booking.paymentAmount.toFixed(2)} · ${booking.paymentStatus}`} />
        {booking.cancellationReason && <InfoCard label="Cancellation Reason" value={booking.cancellationReason} />}
      </div>

      {booking.timeline && booking.timeline.length > 0 && (
        <div>
          <h3 style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.75rem' }}>Status Timeline</h3>
          <div style={{ background: 'white', borderRadius: '8px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
            {booking.timeline.map((entry, i) => (
              <div key={i} style={{ display: 'flex', gap: '1rem', padding: '0.75rem 1rem', borderBottom: i < booking.timeline.length - 1 ? '1px solid #f1f5f9' : 'none' }}>
                <span style={{ padding: '0.2rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 600, background: STATUS_COLORS[entry.status]?.bg ?? '#f1f5f9', color: STATUS_COLORS[entry.status]?.color ?? '#475569', whiteSpace: 'nowrap' }}>
                  {entry.status}
                </span>
                <span style={{ color: '#64748b', fontSize: '0.875rem' }}>{new Date(entry.timestamp).toLocaleString('sl-SI')}</span>
                {entry.note && <span style={{ color: '#475569', fontSize: '0.875rem' }}>— {entry.note}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1rem' }}>
      <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', marginBottom: '0.25rem' }}>{label}</div>
      <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{value}</div>
    </div>
  );
}