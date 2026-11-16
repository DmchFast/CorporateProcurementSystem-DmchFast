import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import PrivateRoute from './components/PrivateRoute';
import Layout from './components/Layout';
import Placeholder from './components/Placeholder';
import LoginPage from './pages/LoginPage';
import ServicesPage from './pages/ServicesPage';
import UsersPage from './pages/UsersPage';

function Protected({ title, roles }) {
    return (
        <PrivateRoute roles={roles}>
            <Layout>
                <Placeholder title={title} />
            </Layout>
        </PrivateRoute>
    );
}

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />

                {/* Главные табы */}
                <Route path="/my-orders" element={<Protected title="Мои заказы" />} />
                <Route
                    path="/services"
                    element={
                        <PrivateRoute>
                            <Layout>
                                <ServicesPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />
                <Route path="/statistics" element={<Protected title="Статистика" />} />

                {/* Подстраницы сервисов */}
                <Route path="/services/catalog" element={<Protected title="Каталог товаров" />} />
                <Route
                    path="/services/orders"
                    element={<Protected title="Заказы" roles={['MANAGER', 'ADMIN']} />}
                />
                <Route path="/services/categories" element={<Protected title="Категории" />} />
                <Route path="/services/warehouses" element={<Protected title="Склады и остатки" />} />
                <Route
                    path="/services/export"
                    element={<Protected title="Экспорт данных" roles={['MANAGER', 'ADMIN']} />}
                />
                <Route
                    path="/services/users"
                    element={
                        <PrivateRoute roles={['ADMIN']}>
                            <Layout>
                                <UsersPage />
                            </Layout>
                        </PrivateRoute>
                    }
                />

                {/* Смена пароля */}
                <Route path="/change-password" element={<Protected title="Смена пароля" />} />

                <Route path="/" element={<Navigate to="/my-orders" replace />} />
                <Route path="*" element={<Navigate to="/my-orders" replace />} />
            </Routes>
        </BrowserRouter>
    );
}