import { useAuth } from '../auth/useAuth';

export function DashboardScreen() {
  const { isAuthenticated } = useAuth();

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Dashboard</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>Zdravdom Admin Portal</p>

      {isAuthenticated ? (
        <div style={{ color: '#16a34a' }}>Authenticated</div>
      ) : (
        <div style={{ color: '#dc2626' }}>Not authenticated</div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem', marginTop: '2rem' }}>
        {['Total Patients', 'Total Providers', 'Active Bookings', 'Revenue Today'].map((label, i) => (
          <div key={i} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1.25rem' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>{label}</div>
            <div style={{ fontSize: '2rem', fontWeight: 700, color: '#1e293b' }}>—</div>
          </div>
        ))}
      </div>
    </div>
  );
}