import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';

function PurchasesStub() {
  const { user, logout } = useAuth();
  return (
      <div style={{ padding: '40px', maxWidth: '800px', margin: '0 auto' }}>
        <div className="page-card">
          <h2>Закупки (заглушка)</h2>
          <p>Вы вошли как <b>{user?.username}</b>, роль: <b>{user?.role}</b></p>
          <button
              onClick={logout}
              style={{
                marginTop: '16px',
                padding: '10px 20px',
                border: 'none',
                borderRadius: '10px',
                background: '#dc2626',
                color: '#fff',
                cursor: 'pointer',
              }}
          >
            Выйти
          </button>
        </div>
      </div>
  );
}

function PrivateGate({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="page-loader">Загрузка...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
              path="/purchases"
              element={
                <PrivateGate>
                  <PurchasesStub />
                </PrivateGate>
              }
          />
          <Route path="/" element={<Navigate to="/purchases" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
  );
}