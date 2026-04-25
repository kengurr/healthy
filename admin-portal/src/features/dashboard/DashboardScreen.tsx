import { useDashboard } from '../../hooks/useAdmin';

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('sl-SI', { style: 'currency', currency: 'EUR' }).format(amount);
}

export function DashboardScreen() {
  const { data, isLoading, isError } = useDashboard();

  if (isLoading) {
    return (
      <div style={{ padding: '2rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Dashboard</h1>
        <p style={{ color: '#64748b', marginBottom: '2rem' }}>Loading...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div style={{ padding: '2rem' }}>
        <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Dashboard</h1>
        <p style={{ color: '#dc2626' }}>Failed to load dashboard data. Is the backend running?</p>
      </div>
    );
  }

  const stats = data ?? {
    totalPatients: 0,
    totalProviders: 0,
    activeProviders: 0,
    totalBookings: 0,
    activeBookings: 0,
    bookingsToday: 0,
    bookingsThisWeek: 0,
    revenueToday: 0,
    revenueThisWeek: 0,
    revenueThisMonth: 0,
    pendingVerifications: 0,
    openEscalations: 0,
  };

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Dashboard</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>Zdravdom Admin Portal — {data?.generatedAt ? `Updated ${new Date(data.generatedAt).toLocaleString('sl-SI')}` : ''}</p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '1rem', marginTop: '2rem' }}>
        <StatCard label="Total Patients" value={stats.totalPatients} />
        <StatCard label="Total Providers" value={stats.totalProviders} />
        <StatCard label="Active Providers" value={stats.activeProviders} />
        <StatCard label="Total Bookings" value={stats.totalBookings} />
        <StatCard label="Active Bookings" value={stats.activeBookings} />
        <StatCard label="Bookings Today" value={stats.bookingsToday} />
        <StatCard label="Bookings This Week" value={stats.bookingsThisWeek} />
        <StatCard label="Revenue Today" value={formatCurrency(stats.revenueToday)} />
        <StatCard label="Revenue This Week" value={formatCurrency(stats.revenueThisWeek)} />
        <StatCard label="Revenue This Month" value={formatCurrency(stats.revenueThisMonth)} />
        <StatCard label="Pending Verifications" value={stats.pendingVerifications} highlight={stats.pendingVerifications > 0} />
        <StatCard label="Open Escalations" value={stats.openEscalations} highlight={stats.openEscalations > 0} />
      </div>
    </div>
  );
}

function StatCard({ label, value, highlight }: { label: string; value: number | string; highlight?: boolean }) {
  return (
    <div style={{
      background: 'white',
      border: `1px solid ${highlight ? '#f59e0b' : '#e2e8f0'}`,
      borderRadius: '8px',
      padding: '1.25rem',
      boxShadow: highlight ? '0 0 0 2px #fef3c7' : undefined,
    }}>
      <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>{label}</div>
      <div style={{ fontSize: '2rem', fontWeight: 700, color: highlight ? '#d97706' : '#1e293b' }}>{value}</div>
    </div>
  );
}