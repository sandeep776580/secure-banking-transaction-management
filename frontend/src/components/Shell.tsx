import { useEffect, useMemo, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Activity, ArrowDownLeft, ArrowLeftRight, ArrowRight, ArrowUpRight, Bell, Building2, ChevronDown, CreditCard, LayoutDashboard, LogOut, Menu, Search, Settings, ShieldCheck, Users, Wallet, X } from 'lucide-react';
import { accountApi, adminApi, transactionApi } from '../api';
import type { Transaction } from '../types/api';
import { useAuth } from '../context/AuthContext';
import { useToast } from './ui';

const customerLinks = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/accounts', label: 'Accounts', icon: Wallet },
  { to: '/app/transactions', label: 'Transactions', icon: Activity },
  { to: '/app/statement', label: 'Statements', icon: CreditCard },
];
const adminLinks = [
  { to: '/admin', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/admin/users', label: 'Customers', icon: Users },
  { to: '/admin/accounts', label: 'Accounts', icon: Building2 },
  { to: '/admin/transactions', label: 'Transactions', icon: Activity },
];
export function AppShell({ admin = false }: { admin?: boolean }) {
  const [open, setOpen] = useState(false); const [searchOpen, setSearchOpen] = useState(false); const [activityOpen, setActivityOpen] = useState(false); const [activityVersion, setActivityVersion] = useState(0); const [query, setQuery] = useState(''); const [activity, setActivity] = useState<Transaction[]>([]); const [activityLoading, setActivityLoading] = useState(true); const { user, logout } = useAuth(); const toast = useToast(); const navigate = useNavigate(); const location = useLocation();
  const links = admin ? adminLinks : customerLinks;
  const shortcuts = useMemo(() => (admin ? [
    { label: 'Admin overview', detail: 'Platform statistics', to: '/admin', icon: LayoutDashboard },
    { label: 'Customers', detail: 'Search registered users', to: '/admin/users', icon: Users },
    { label: 'Account oversight', detail: 'Balances and statuses', to: '/admin/accounts', icon: Building2 },
    { label: 'Transaction monitoring', detail: 'Review platform activity', to: '/admin/transactions', icon: Activity },
  ] : [
    { label: 'Overview', detail: 'Your financial dashboard', to: '/app', icon: LayoutDashboard },
    { label: 'Accounts', detail: 'Balances and account details', to: '/app/accounts', icon: Wallet },
    { label: 'Transactions', detail: 'Search recent activity', to: '/app/transactions', icon: Activity },
    { label: 'Statements', detail: 'Dates and account records', to: '/app/statement', icon: CreditCard },
    { label: 'Add money', detail: 'Demo deposit', to: '/app/deposit', icon: ArrowDownLeft },
    { label: 'Withdraw', detail: 'Move money out', to: '/app/withdraw', icon: ArrowUpRight },
    { label: 'Transfer', detail: 'Move between accounts', to: '/app/transfer', icon: ArrowLeftRight },
    { label: 'Profile and settings', detail: 'Your personal details', to: '/app/profile', icon: Settings },
  ]), [admin]);
  const visibleShortcuts = shortcuts.filter((item) => `${item.label} ${item.detail}`.toLowerCase().includes(query.trim().toLowerCase()));
  const currentName = [...customerLinks, ...adminLinks,
    { to: '/app/deposit', label: 'Deposit', icon: ArrowDownLeft }, { to: '/app/withdraw', label: 'Withdraw', icon: ArrowUpRight },
    { to: '/app/transfer', label: 'Transfer', icon: ArrowLeftRight }, { to: '/app/profile', label: 'Profile', icon: Settings },
  ].find((item) => item.to === location.pathname)?.label || (admin ? 'Control center' : 'Overview');
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); setSearchOpen(true); }
      if (event.key === 'Escape') { setSearchOpen(false); setActivityOpen(false); }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);
  useEffect(() => {
    let cancelled = false;
    const loadActivity = async () => {
      try {
        if (admin) {
          const { data } = await adminApi.transactions({ page: 0, size: 5 });
          if (!cancelled) setActivity(data.content);
        } else {
          const { data: accounts } = await accountApi.list();
          const pages = await Promise.all(accounts.map((account) => transactionApi.history({ accountId: account.id, page: 0, size: 5 })));
          const recent = pages.flatMap((page) => page.data.content).sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)).slice(0, 5);
          if (!cancelled) setActivity(recent);
        }
      } catch { if (!cancelled) setActivity([]); }
      finally { if (!cancelled) setActivityLoading(false); }
    };
    void loadActivity();
    return () => { cancelled = true; };
  }, [admin, activityVersion]);
  const signOut = async () => { await logout(); toast('You have been securely signed out.'); navigate('/login'); };
  return <div className="app-shell">
    <aside className={`sidebar ${open ? 'sidebar-open' : ''}`}>
      <div className="brand"><div className="brand-mark"><span>n</span></div><span>northstar<span className="brand-dot">.</span></span><button className="mobile-close" onClick={() => setOpen(false)} aria-label="Close menu"><X/></button></div>
      <div className="workspace-label">{admin ? 'ADMINISTRATION' : 'YOUR SPACE'}</div>
      <nav className="side-nav">{links.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><Icon size={18}/><span>{label}</span>{label === 'Transactions' && <span className="nav-arrow">↗</span>}</NavLink>)}</nav>
      {!admin && <><div className="workspace-label space-top">MOVE MONEY</div><nav className="side-nav"><NavLink to="/app/deposit" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><ArrowDownLeft size={18}/><span>Deposit</span></NavLink><NavLink to="/app/withdraw" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><ArrowUpRight size={18}/><span>Withdraw</span></NavLink><NavLink to="/app/transfer" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><ArrowLeftRight size={18}/><span>Transfer</span></NavLink></nav></>}
      {admin && <div className="admin-note"><ShieldCheck size={19}/><div><b>Admin workspace</b><span>Privileged access</span></div></div>}
      <div className="sidebar-bottom"><div className="help-card"><div className="help-orbit">✦</div><b>Here for you</b><p>Need a hand with your finances?</p><a href="mailto:support@example.com">Visit support <span>↗</span></a></div><NavLink to={admin ? '/admin' : '/app/profile'} className="nav-link settings-link"><Settings size={18}/><span>Settings</span></NavLink><button className="profile-mini" onClick={() => navigate(admin ? '/admin' : '/app/profile')}><div className="avatar">{user?.fullName?.slice(0, 1).toUpperCase() || 'U'}</div><div className="profile-mini-copy"><b>{user?.fullName || 'Account'}</b><span>{admin ? 'Administrator' : 'Personal account'}</span></div><ChevronDown size={16}/></button></div>
    </aside>
    {open && <button className="sidebar-scrim" aria-label="Close navigation" onClick={() => setOpen(false)}/>}
    <main className="main-area"><header className="topbar"><button className="menu-toggle" aria-label="Open navigation" onClick={() => setOpen(true)}><Menu/></button><div className="topbar-crumb"><span>{admin ? 'Administration' : 'Personal banking'}</span><span className="crumb-slash">/</span><b>{currentName}</b></div><div className="topbar-actions"><button className="icon-button top-search" aria-label="Search pages" title="Search pages (Ctrl K)" onClick={() => { setQuery(''); setSearchOpen(true); }}><Search size={18}/></button><button className={`icon-button notification ${activityOpen ? 'active' : ''}`} aria-label="Recent activity" aria-expanded={activityOpen} onClick={() => { const next = !activityOpen; setActivityOpen(next); setSearchOpen(false); if (next) { setActivityLoading(true); setActivityVersion((version) => version + 1); } }}><Bell size={18}/></button><button className="top-avatar" title="Profile" onClick={() => navigate(admin ? '/admin' : '/app/profile')}>{user?.fullName?.slice(0, 1).toUpperCase()}</button>
      {activityOpen && <div className="activity-popover"><div className="popover-heading"><div><b>Recent activity</b><span>Latest transactions on your account</span></div><button onClick={() => setActivityOpen(false)} aria-label="Close activity"><X size={16}/></button></div>{activityLoading ? <div className="popover-loading"><i/><i/><i/></div> : activity.length ? <div className="popover-items">{activity.map((item) => { const incoming = item.type === 'DEPOSIT' || item.type === 'TRANSFER_IN'; return <div className="popover-item" key={item.id}><span className={`popover-icon ${incoming ? 'incoming' : 'outgoing'}`}>{incoming ? <ArrowDownLeft size={15}/> : <ArrowUpRight size={15}/>}</span><span className="popover-item-copy"><b>{item.type.replace('_', ' ')}</b><small>{new Date(item.createdAt).toLocaleString()}</small></span><strong className={incoming ? 'incoming' : ''}>{incoming ? '+' : '−'}{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(item.amount)}</strong></div>; })}</div> : <div className="popover-empty"><Activity size={20}/><b>No recent activity</b><span>Your latest transactions will appear here.</span></div>}<button className="popover-footer" onClick={() => { setActivityOpen(false); navigate(admin ? '/admin/transactions' : '/app/transactions'); }}>View all activity <ArrowRight size={14}/></button></div>}
      </div></header><div className="content-area"><Outlet/></div><footer className="app-footer"><span>© {new Date().getFullYear()} Northstar Financial</span><span><ShieldCheck size={13}/> Your money is protected</span><button onClick={signOut}><LogOut size={14}/> Sign out</button></footer></main>
    {searchOpen && <div className="command-scrim" onMouseDown={(event) => { if (event.target === event.currentTarget) setSearchOpen(false); }}><section className="command-palette" role="dialog" aria-modal="true" aria-label="Search navigation"><div className="command-input"><Search size={19}/><input autoFocus placeholder="Where would you like to go?" value={query} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && visibleShortcuts[0]) { setSearchOpen(false); navigate(visibleShortcuts[0].to); } }}/><kbd>ESC</kbd></div><div className="command-label">{query ? 'MATCHING PAGES' : 'QUICK NAVIGATION'}</div><div className="command-results">{visibleShortcuts.length ? visibleShortcuts.map(({ label, detail, to, icon: Icon }) => <button key={to} onClick={() => { setSearchOpen(false); navigate(to); }}><span className="command-result-icon"><Icon size={17}/></span><span><b>{label}</b><small>{detail}</small></span><ArrowRight size={15}/></button>) : <div className="command-no-results">No pages match “{query}”</div>}</div><div className="command-hint"><span><kbd>↵</kbd> Open first result</span><span><kbd>ESC</kbd> Close</span></div></section></div>}
  </div>;
}
