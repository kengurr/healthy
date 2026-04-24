import { useState } from 'react';
import { useServices } from '../../hooks/useAdmin';
import { adminApi } from '@zdravdom/api-client';
import type { ServiceCategory } from '@zdravdom/types';

const CATEGORIES: ServiceCategory[] = [
  'NURSING', 'THERAPY', 'LABORATORY', 'SPECIALIST',
];

export function ServicesScreen() {
  const { data: services, isLoading, refetch } = useServices();
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [category, setCategory] = useState<ServiceCategory>('NURSING');
  const [description, setDescription] = useState('');
  const [durationMinutes, setDurationMinutes] = useState('');
  const [price, setPrice] = useState('');
  const [includedItems, setIncludedItems] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    try {
      await adminApi.createService({
        name,
        category,
        description,
        durationMinutes: parseInt(durationMinutes),
        price: parseFloat(price),
        includedItems: includedItems.split('\n').map(s => s.trim()).filter(Boolean),
        requiredDocuments: [],
      });
      setSuccess('Service created successfully');
      setCreating(false);
      setName(''); setCategory('NURSING'); setDescription('');
      setDurationMinutes(''); setPrice(''); setIncludedItems('');
      refetch();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to create service');
    }
  }

  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Services & Pricing</h1>
          <p style={{ color: '#64748b', marginBottom: 0 }}>Manage service catalog and packages</p>
        </div>
        <button
          onClick={() => setCreating(true)}
          style={{ padding: '0.5rem 1rem', background: '#1e40af', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600 }}
        >
          + New Service
        </button>
      </div>

      {/* Create form */}
      {creating && (
        <form onSubmit={handleCreate} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1.5rem', marginTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <h3 style={{ fontWeight: 600, marginBottom: '0.25rem' }}>Create New Service</h3>
          {error && <p style={{ color: '#dc2626', fontSize: '0.875rem' }}>{error}</p>}
          {success && <p style={{ color: '#16a34a', fontSize: '0.875rem' }}>{success}</p>}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <input placeholder="Service name" value={name} onChange={e => setName(e.target.value)} required style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }} />
            <select value={category} onChange={e => setCategory(e.target.value as ServiceCategory)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
              {CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}
            </select>
            <input placeholder="Duration (minutes)" type="number" value={durationMinutes} onChange={e => setDurationMinutes(e.target.value)} required style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }} />
            <input placeholder="Price (EUR)" type="number" step="0.01" value={price} onChange={e => setPrice(e.target.value)} required style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0' }} />
          </div>
          <textarea placeholder="Description" value={description} onChange={e => setDescription(e.target.value)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0', resize: 'vertical', minHeight: '60px' }} />
          <textarea placeholder="Included items (one per line)" value={includedItems} onChange={e => setIncludedItems(e.target.value)} style={{ padding: '0.5rem', borderRadius: '6px', border: '1px solid #e2e8f0', resize: 'vertical', minHeight: '60px' }} />
          <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
            <button type="button" onClick={() => setCreating(false)} style={{ padding: '0.5rem 1rem', background: '#e2e8f0', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem' }}>Cancel</button>
            <button type="submit" style={{ padding: '0.5rem 1rem', background: '#16a34a', color: 'white', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '0.875rem', fontWeight: 600 }}>Create Service</button>
          </div>
        </form>
      )}

      {/* Services list */}
      {isLoading && <p style={{ color: '#64748b', marginTop: '1rem' }}>Loading...</p>}

      {!isLoading && services && services.length === 0 && (
        <div style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '2rem', textAlign: 'center', color: '#94a3b8', marginTop: '1rem' }}>No services found.</div>
      )}

      {!isLoading && services && services.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1rem', marginTop: '1.5rem' }}>
          {services.map(s => (
            <div key={s.id} style={{ background: 'white', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1.25rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <h3 style={{ fontWeight: 600, fontSize: '1rem' }}>{s.name}</h3>
                <span style={{ fontSize: '0.7rem', background: '#f1f5f9', color: '#64748b', padding: '0.2rem 0.5rem', borderRadius: '4px' }}>{s.category}</span>
              </div>
              {s.description && <p style={{ color: '#64748b', fontSize: '0.875rem', marginBottom: '0.75rem', lineHeight: 1.4 }}>{s.description}</p>}
              <div style={{ display: 'flex', gap: '1rem', fontSize: '0.875rem', color: '#475569' }}>
                <span>€{s.price}</span>
                <span>·</span>
                <span>{s.duration} min</span>
                {s.rating != null && <><span>·</span><span>★ {s.rating.toFixed(1)}</span></>}
              </div>
              {s.includedItems && s.includedItems.length > 0 && (
                <ul style={{ margin: '0.5rem 0 0', paddingLeft: '1.25rem', fontSize: '0.8rem', color: '#64748b' }}>
                  {s.includedItems.slice(0, 3).map((item, i) => <li key={i}>{item}</li>)}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
