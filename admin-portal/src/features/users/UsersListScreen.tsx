import { useState } from 'react';
import { useUsers } from '../../hooks/useAdmin';

export function UsersListScreen() {
  const [statusFilter, setStatusFilter] = useState<string>('');
  const { data: users, isLoading } = useUsers();

  const filtered = users?.filter(u => {
    if (statusFilter === 'verified') return u.verified;
    if (statusFilter === 'unverified') return !u.verified;
    if (statusFilter === 'active') return u.active;
    if (statusFilter === 'inactive') return !u.active;
    return true;
  });

  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>User Management</h1>
      <p style={{ color: '#64748b', marginBottom: '2rem' }}>Manage patient accounts</p>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
        <select
          value={statusFilter}
          onChange={e => setStatusFilter(e.target.value)}
          style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }}
        >
          <option value="">All users</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
          <option value="verified">Verified</option>
          <option value="unverified">Unverified</option>
        </select>
      </div>

      {isLoading && <p style={{ color: '#64748b' }}>Loading...</p>}

      {!isLoading && filtered && filtered.length === 0 && (
        <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#94a3b8' }}>
          No users found.
        </div>
      )}

      {!isLoading && filtered && filtered.length > 0 && (
        <table style={{ width: '100%', borderCollapse: 'collapse', background: 'white', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>ID</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Name</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Email</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Phone</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Date of Birth</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Verified</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Status</th>
              <th style={{ padding: '0.75rem 1rem', textAlign: 'left', fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase' }}>Joined</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(u => (
              <tr key={u.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '0.75rem 1rem', color: '#94a3b8', fontSize: '0.875rem' }}>#{u.id}</td>
                <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>{u.firstName} {u.lastName}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569' }}>{u.email}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{u.phone ?? '—'}</td>
                <td style={{ padding: '0.75rem 1rem', color: '#475569', fontSize: '0.875rem' }}>{u.dateOfBirth ?? '—'}</td>
                <td style={{ padding: '0.75rem 1rem' }}>
                  <span style={{
                    padding: '0.25rem 0.5rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: u.verified ? '#dcfce7' : '#fef9c3',
                    color: u.verified ? '#166534' : '#854d0e',
                  }}>{u.verified ? 'Verified' : 'Pending'}</span>
                </td>
                <td style={{ padding: '0.75rem 1rem' }}>
                  <span style={{
                    padding: '0.25rem 0.5rem',
                    borderRadius: '9999px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    background: u.active ? '#dcfce7' : '#fee2e2',
                    color: u.active ? '#166534' : '#991b1b',
                  }}>{u.active ? 'Active' : 'Inactive'}</span>
                </td>
                <td style={{ padding: '0.75rem 1rem', color: '#64748b', fontSize: '0.875rem' }}>
                  {u.createdAt ? new Date(u.createdAt).toLocaleDateString('sl-SI') : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}