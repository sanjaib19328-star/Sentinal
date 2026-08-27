import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { LayoutDashboard, Layers, Compass, FileText, ShieldCheck, LogOut, ExternalLink, Sparkles } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

interface SidebarProps {
  isOpen: boolean;
  onCloseMobile?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onCloseMobile }) => {
  const { user, logout } = useAuth();

  const navItems = [
    { to: '/dashboard', label: 'Executive Dashboard', icon: LayoutDashboard },
    { to: '/applications', label: 'Applications', icon: Layers },
    { to: '/apis', label: 'API Catalog', icon: Compass },
    { to: '/assistant', label: 'AI Assistant', icon: Sparkles },
    { to: '/requests', label: 'Request Explorer', icon: FileText },
  ];

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          onClick={onCloseMobile}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.4)',
            backdropFilter: 'blur(2px)',
            zIndex: 40,
            display: 'block',
          }}
          className="mobile-backdrop"
        />
      )}

      <aside
        style={{
          width: '260px',
          backgroundColor: 'var(--bg-surface)',
          borderRight: '1px solid var(--border-color)',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          flexShrink: 0,
          position: 'sticky',
          top: 0,
          height: '100vh',
          zIndex: 45,
          transition: 'transform 0.2s ease-in-out',
        }}
        className={`sidebar ${isOpen ? 'open' : ''}`}
      >
        {/* Brand / Logo */}
        <div>
          <Link
            to="/dashboard"
            style={{
              padding: '1.25rem 1.5rem',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              borderBottom: '1px solid var(--border-color)',
              textDecoration: 'none',
              cursor: 'pointer',
            }}
          >
            <div
              style={{
                width: '2rem',
                height: '2rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--primary-light)',
                color: 'var(--primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '1px solid var(--primary-border)',
              }}
            >
              <ShieldCheck style={{ width: '1.25rem', height: '1.25rem' }} />
            </div>
            <div>
              <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
                SENTINEL
              </span>
              <span
                style={{
                  display: 'block',
                  fontSize: '0.6875rem',
                  fontWeight: 600,
                  color: 'var(--primary)',
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                }}
              >
                Observability
              </span>
            </div>
          </Link>

          {/* Navigation Items */}
          <nav style={{ padding: '1rem 0.75rem' }}>
            <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', padding: '0.5rem 0.75rem', letterSpacing: '0.05em' }}>
              Platform
            </div>
            <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
              {navItems.map((item) => {
                const Icon = item.icon;
                return (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={onCloseMobile}
                      style={({ isActive }) => ({
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.75rem',
                        padding: '0.625rem 0.75rem',
                        borderRadius: 'var(--radius-md)',
                        fontSize: '0.875rem',
                        fontWeight: isActive ? 600 : 500,
                        textDecoration: 'none',
                        color: isActive ? 'var(--primary)' : 'var(--text-secondary)',
                        backgroundColor: isActive ? 'var(--primary-light)' : 'transparent',
                        transition: 'all 0.15s ease',
                      })}
                    >
                      <Icon style={{ width: '1.125rem', height: '1.125rem' }} />
                      <span>{item.label}</span>
                    </NavLink>
                  </li>
                );
              })}
            </ul>

            <div style={{ fontSize: '0.6875rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', padding: '1.25rem 0.75rem 0.5rem', letterSpacing: '0.05em' }}>
              External
            </div>
            <ul style={{ listStyle: 'none' }}>
              <li>
                <a
                  href="http://localhost:8080/actuator/health"
                  target="_blank"
                  rel="noreferrer"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.625rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    fontSize: '0.875rem',
                    fontWeight: 500,
                    textDecoration: 'none',
                    color: 'var(--text-secondary)',
                  }}
                >
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <ShieldCheck style={{ width: '1.125rem', height: '1.125rem' }} />
                    Actuator Health
                  </span>
                  <ExternalLink style={{ width: '0.875rem', height: '0.875rem', color: 'var(--text-muted)' }} />
                </a>
              </li>
            </ul>
          </nav>
        </div>

        {/* User Info & Logout */}
        <div style={{ padding: '1rem', borderTop: '1px solid var(--border-color)', backgroundColor: '#fafafa' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-primary)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                {user?.name || 'Operator'}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                {user?.email}
              </div>
            </div>
          </div>
          <button
            onClick={logout}
            className="btn btn-secondary btn-sm"
            style={{ width: '100%', justifyContent: 'flex-start', color: 'var(--danger)' }}
          >
            <LogOut style={{ width: '0.875rem', height: '0.875rem' }} />
            Sign Out
          </button>
        </div>
      </aside>

      <style>{`
        @media (max-width: 768px) {
          .sidebar {
            position: fixed !important;
            transform: translateX(-100%);
          }
          .sidebar.open {
            transform: translateX(0);
          }
        }
      `}</style>
    </>
  );
};
