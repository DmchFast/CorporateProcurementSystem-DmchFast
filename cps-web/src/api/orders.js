import { apiRequest } from './client';

const USE_MOCKS = true;

const MOCK_ORDERS = [
    {
        id: '1001',
        status: 'CREATED',
        notes: 'Закупка канцтоваров для офиса',
        createdAt: '2024-11-14T09:15:00Z',
        updatedAt: '2025-11-14T09:15:00Z',
        itemsCount: 4,
        total: 12450.5,
    },
    {
        id: '1002',
        status: 'APPROVED',
        notes: 'Ноутбуки для разработчиков',
        createdAt: '2025-11-12T14:30:00Z',
        updatedAt: '2026-11-13T10:00:00Z',
        itemsCount: 3,
        total: 289000.0,
    },
    {
        id: '1003',
        status: 'DELIVERED',
        notes: null,
        createdAt: '2025-11-08T11:00:00Z',
        updatedAt: '2026-11-10T16:45:00Z',
        itemsCount: 1,
        total: 8990.0,
    },
    {
        id: '1004',
        status: 'CANCELLED',
        notes: 'Отменён по просьбе заказчика',
        createdAt: '2025-11-05T08:20:00Z',
        updatedAt: '2026-11-06T12:00:00Z',
        itemsCount: 2,
        total: 3400.0,
    },
    {
        id: '1005',
        status: 'CREATED',
        notes: 'Кулер и вода на месяц',
        createdAt: '2023-11-15T16:00:00Z',
        updatedAt: '2024-11-15T16:00:00Z',
        itemsCount: 2,
        total: 5600.0,
    },
    {
        id: '1006',
        status: 'APPROVED',
        notes: 'Офисные кресла — 5 штук',
        createdAt: '2026-11-10T12:00:00Z',
        updatedAt: '2026-11-11T09:30:00Z',
        itemsCount: 1,
        total: 42500.0,
    },
    {
        id: '1007',
        status: 'DELIVERED',
        notes: null,
        createdAt: '2025-10-30T10:00:00Z',
        updatedAt: '2025-11-02T14:15:00Z',
        itemsCount: 3,
        total: 17300.75,
    },
    {
        id: '1008',
        status: 'CREATED',
        notes: 'Корпоративная печать, визитки',
        createdAt: '2024-09-16T08:45:00Z',
        updatedAt: '2025-10-16T08:45:00Z',
        itemsCount: 2,
        total: 7800.0,
    },
];

export async function getMyOrders() {
    if (USE_MOCKS) {
        // Имитация задержки сети
        await new Promise((r) => setTimeout(r, 200));
        return MOCK_ORDERS.slice();
    }
    const response = await apiRequest('/orders/my');
    if (!response.ok) {
        throw new Error('Не удалось загрузить заказы');
    }
    return response.json();
}