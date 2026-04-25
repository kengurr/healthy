import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../features/auth/useAuth';
import type { UserRole } from '@zdravdom/api-client';

const NAV_ITEMS: { to: string; label: string; roles: UserRole[] }[] = [
  { to: '/dashboard', label: 'Dashboard', roles: ['OPERATOR', 'ADMIN', 'SUPERADMIN'] },
  { to: '/providers', label: 'Providers', roles: ['OPERATOR', 'ADMIN', 'SUPERADMIN'] },
  { to: '/bookings', label: 'Bookings', roles: ['OPERATOR', 'ADMIN', 'SUPERADMIN'] },
  { to: '/escalations', label: 'Escalations', roles: ['OPERATOR', 'ADMIN', 'SUPERADMIN'] },
  { to: '/users', label: 'Users', roles: ['ADMIN', 'SUPERADMIN'] },
  { to: '/services', label: 'Services', roles: ['ADMIN', 'SUPERADMIN'] },
];

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { logout, role } = useAuth();
  const location = useLocation();

  const visibleItems = NAV_ITEMS.filter(item => role && item.roles.includes(role));

  return (
    <div style={{ display: 'flex', minHeight: '100vh' }}>
      {/* Sidebar */}
      <aside style={{ width: '220px', background: '#1e293b', color: 'white', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '1.25rem', fontSize: '1.25rem', fontWeight: 700, borderBottom: '1px solid #334155' }}>
          Zdravdom
        </div>
        <nav style={{ flex: 1, padding: '0.5rem' }}>
          {visibleItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              style={{
                display: 'block',
                padding: '0.625rem 0.75rem',
                color: location.pathname === item.to ? '#93c5fd' : '#cbd5e1',
                textDecoration: 'none',
                borderRadius: '4px',
                fontSize: '0.9rem',
                fontWeight: location.pathname === item.to ? 600 : 400,
                background: location.pathname === item.to ? '#334155' : 'transparent',
                marginBottom: '2px',
              }}
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div style={{ padding: '1rem', borderTop: '1px solid #334155' }}>
          <button
            onClick={logout}
            style={{ width: '100%', padding: '0.5rem', background: 'transparent', border: '1px solid #475569', color: '#cbd5e1', borderRadius: '4px', cursor: 'pointer', fontSize: '0.875rem' }}
          >
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main style={{ flex: 1, overflow: 'auto' }}>
        {children}
      </main>
    </div>
  );
}