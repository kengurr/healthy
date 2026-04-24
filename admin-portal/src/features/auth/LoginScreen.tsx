import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { authApi, setAuthTokens } from '@zdravdom/api-client';
import { useNavigate } from 'react-router-dom';

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginScreen() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginForm) => {
    setError(null);
    try {
      const response = await authApi.login(data.email, data.password);
      setAuthTokens({
        accessToken: response.data.accessToken,
        refreshToken: response.data.refreshToken,
        expiresIn: response.data.expiresIn,
      });
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr.response?.data?.message ?? 'Login failed. Please try again.');
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f5f5' }}>
      <div style={{ background: 'white', padding: '2rem', borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)', width: '100%', maxWidth: '400px' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 600, marginBottom: '1.5rem', textAlign: 'center' }}>Zdravdom Admin</h1>
        <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {error && (
            <div style={{ background: '#fee', color: '#c00', padding: '0.75rem', borderRadius: '4px', fontSize: '0.875rem' }}>
              {error}
            </div>
          )}
          <div>
            <label htmlFor="email" style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '0.25rem' }}>Email</label>
            <input
              id="email"
              type="email"
              {...register('email')}
              style={{ width: '100%', padding: '0.5rem', border: '1px solid #ccc', borderRadius: '4px', fontSize: '1rem' }}
            />
            {errors.email && <span style={{ color: '#c00', fontSize: '0.75rem' }}>{errors.email.message}</span>}
          </div>
          <div>
            <label htmlFor="password" style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '0.25rem' }}>Password</label>
            <input
              id="password"
              type="password"
              {...register('password')}
              style={{ width: '100%', padding: '0.5rem', border: '1px solid #ccc', borderRadius: '4px', fontSize: '1rem' }}
            />
            {errors.password && <span style={{ color: '#c00', fontSize: '0.75rem' }}>{errors.password.message}</span>}
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            style={{ padding: '0.75rem', background: '#2563eb', color: 'white', border: 'none', borderRadius: '4px', fontSize: '1rem', fontWeight: 500, cursor: isSubmitting ? 'not-allowed' : 'pointer', opacity: isSubmitting ? 0.6 : 1 }}
          >
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}