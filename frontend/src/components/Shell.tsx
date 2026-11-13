import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Activity, ArrowDownLeft, ArrowLeftRight, ArrowUpRight, Bell, Building2, ChevronDown, CreditCard, LayoutDashboard, LogOut, Menu, Search, Settings, ShieldCheck, Users, Wallet, X } from 'lucide-react';
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
  const [open, setOpen] = useState(false); const { user, logout } = useAuth(); const toast = useToast(); const navigate = useNavigate();
  const links = admin ? adminLinks : customerLinks;
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
    <main className="main-area"><header className="topbar"><button className="menu-toggle" aria-label="Open navigation" onClick={() => setOpen(true)}><Menu/></button><div className="topbar-crumb"><span>{admin ? 'Administration' : 'Personal banking'}</span><span className="crumb-slash">/</span><b>{admin ? 'Control center' : 'Overview'}</b></div><div className="topbar-actions"><button className="icon-button top-search" aria-label="Search"><Search size={18}/></button><button className="icon-button notification" aria-label="Notifications"><Bell size={18}/><i/></button><button className="top-avatar" title="Profile">{user?.fullName?.slice(0, 1).toUpperCase()}</button></div></header><div className="content-area"><Outlet/></div><footer className="app-footer"><span>© 2025 Northstar Financial</span><span><ShieldCheck size={13}/> Your money is protected</span><button onClick={signOut}><LogOut size={14}/> Sign out</button></footer></main>
  </div>;
}
