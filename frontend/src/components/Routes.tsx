import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
export function CustomerGuard() { const { user, ready } = useAuth(); const location = useLocation(); if (!ready) return null; return user ? <Outlet/> : <Navigate to="/login" state={{ from: location.pathname }} replace/>; }
export function AdminGuard() { const { user, ready } = useAuth(); if (!ready) return null; if (!user) return <Navigate to="/login" replace/>; return user.roles?.includes('ADMIN') ? <Outlet/> : <Navigate to="/app" replace/>; }
export function GuestOnly() { const { user, ready } = useAuth(); if (!ready) return null; return user ? <Navigate to={user.roles?.includes('ADMIN') ? '/admin' : '/app'} replace/> : <Outlet/>; }
