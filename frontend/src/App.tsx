import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ToastProvider } from './components/ui';
import { AdminGuard, CustomerGuard, GuestOnly } from './components/Routes';
import { AppShell } from './components/Shell';
import { LandingPage } from './pages/LandingPage';
import { LoginPage, RegisterPage } from './pages/AuthPages';
import { AccountsPage, DashboardPage, MoneyActionPage, ProfilePage, StatementPage, TransactionsPage, TransferPage } from './pages/CustomerPages';
import { AdminAccountsPage, AdminDashboardPage, AdminTransactionsPage, AdminUsersPage } from './pages/AdminPages';

function IndexRedirect() { const { user } = useAuth(); return <Navigate to={user ? (user.roles.includes('ADMIN') ? '/admin' : '/app') : '/'} replace/>; }
function RouteTree() { return <BrowserRouter><AuthProvider><ToastProvider><Routes><Route path="/" element={<LandingPage/>}/><Route element={<GuestOnly/>}><Route path="/login" element={<LoginPage/>}/><Route path="/register" element={<RegisterPage/>}/></Route><Route element={<CustomerGuard/>}><Route path="/app" element={<AppShell/>}><Route index element={<DashboardPage/>}/><Route path="accounts" element={<AccountsPage/>}/><Route path="transactions" element={<TransactionsPage/>}/><Route path="deposit" element={<MoneyActionPage mode="deposit"/>}/><Route path="withdraw" element={<MoneyActionPage mode="withdraw"/>}/><Route path="transfer" element={<TransferPage/>}/><Route path="statement" element={<StatementPage/>}/><Route path="profile" element={<ProfilePage/>}/></Route></Route><Route element={<AdminGuard/>}><Route path="/admin" element={<AppShell admin/>}><Route index element={<AdminDashboardPage/>}/><Route path="users" element={<AdminUsersPage/>}/><Route path="accounts" element={<AdminAccountsPage/>}/><Route path="transactions" element={<AdminTransactionsPage/>}/></Route></Route><Route path="*" element={<IndexRedirect/>}/></Routes></ToastProvider></AuthProvider></BrowserRouter>; }
export default function App() { return <RouteTree/>; }
