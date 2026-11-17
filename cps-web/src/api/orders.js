import { apiRequest } from './client';

const USE_MOCKS = true;

let mockOrders = [
    {
        id: '1',
        userId: '1',
        username: 'dim',
        status: 'CREATED',
        notes: 'Закупка канцтоваров для офиса',
        createdAt: '2025-11-14T09:15:00Z',
        updatedAt: '2025-11-14T09:15:00Z',
        items: [
            { productId: '3001', productName: 'Ноутбук Lenovo ThinkPad', sku: 'NTB-001', quantity: 1, purchasePrice: 89990.0 },
            { productId: '3002', productName: 'Мышь Logitech MX Master', sku: 'MOU-002', quantity: 2, purchasePrice: 4500.0 },
            { productId: '3003', productName: 'Бумага А4', sku: 'PAP-003', quantity: 10, purchasePrice: 420.5 },
        ],
    },
    {
        id: '2',
        userId: '2',
        username: 'nik',
        status: 'APPROVED',
        notes: 'Ноутбуки для разработчиков',
        createdAt: '2025-11-12T14:30:00Z',
        updatedAt: '2025-11-13T10:00:00Z',
        items: [
            { productId: '3001', productName: 'Ноутбук Lenovo ThinkPad', sku: 'NTB-001', quantity: 3, purchasePrice: 89990.0 },
        ],
    },
    {
        id: '3',
        userId: '1',
        username: 'ivan_sid',
        status: 'DELIVERED',
        notes: null,
        createdAt: '2025-11-08T11:00:00Z',
        updatedAt: '2025-11-10T16:45:00Z',
        items: [
            { productId: '3004', productName: 'Ручка шариковая', sku: 'PEN-004', quantity: 300, purchasePrice: 35.0 },
        ],
    },
    {
        id: '4',
        userId: '3',
        username: 'mat',
        status: 'CANCELLED',
        notes: 'Отменён по просьбе заказчика',
        createdAt: '2025-11-05T08:20:00Z',
        updatedAt: '2025-11-06T12:00:00Z',
        items: [
            { productId: '3008', productName: 'Блокнот А5', sku: 'NOT-008', quantity: 20, purchasePrice: 180.0 },
        ],
    },
    {
        id: '5',
        userId: '2',
        username: 'nik',
        status: 'CREATED',
        notes: 'Кулер и вода на месяц',
        createdAt: '2025-11-15T16:00:00Z',
        updatedAt: '2025-11-15T16:00:00Z',
        items: [
            { productId: '3005', productName: 'Кресло офисное', sku: 'CHR-005', quantity: 1, purchasePrice: 18500.0 },
            { productId: '3006', productName: 'Картридж HP 26A', sku: 'CRT-006', quantity: 2, purchasePrice: 8200.0 },
        ],
    },
    {
        id: '6',
        userId: '3',
        username: 'mat',
        status: 'APPROVED',
        notes: 'Офисные кресла — 5 штук',
        createdAt: '2025-11-10T12:00:00Z',
        updatedAt: '2025-11-11T09:30:00Z',
        items: [
            { productId: '3005', productName: 'Кресло офисное', sku: 'CHR-005', quantity: 5, purchasePrice: 8500.0 },
        ],
    },
];

export function orderTotal(order) {
    return order.items.reduce(
        (sum, item) => sum + item.purchasePrice * item.quantity,
        0
    );
}

export function orderItemsCount(order) {
    return order.items.reduce((sum, item) => sum + item.quantity, 0);
}

export async function getMyOrders() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return mockOrders.map((o) => ({
            ...o,
            itemsCount: orderItemsCount(o),
            total: orderTotal(o),
        }));
    }
    const response = await apiRequest('/orders?userId=me');
    if (!response.ok) {
        throw new Error('Не удалось загрузить заказы');
    }
    return response.json();
}

export async function getAllOrders() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 200));
        return mockOrders.map((o) => ({
            ...o,
            itemsCount: orderItemsCount(o),
            total: orderTotal(o),
        }));
    }
    const response = await apiRequest('/orders');
    if (!response.ok) {
        throw new Error('Не удалось загрузить заказы');
    }
    return response.json();
}

export async function updateOrderNotes(orderId, notes, userId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        const order = mockOrders.find((o) => o.id === orderId);
        if (!order) throw new Error('Заказ не найден');
        if (order.userId !== userId) {
            throw new Error('Можно редактировать только свои заказы');
        }
        mockOrders = mockOrders.map((o) =>
            o.id === orderId
                ? { ...o, notes, updatedAt: new Date().toISOString() }
                : o
        );
        return;
    }
    const response = await apiRequest('/orders/' + orderId + '/notes', {
        method: 'PATCH',
        body: JSON.stringify({ userId, notes }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось сохранить примечание');
    }
}

export async function changeOrderStatus(orderId, status, managerId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        const order = mockOrders.find((o) => o.id === orderId);
        if (!order) throw new Error('Заказ не найден');
        if (order.status === 'DELIVERED') {
            throw new Error('Доставленный заказ нельзя перевести в другой статус');
        }
        mockOrders = mockOrders.map((o) =>
            o.id === orderId
                ? { ...o, status, updatedAt: new Date().toISOString() }
                : o
        );
        return;
    }
    const response = await apiRequest('/orders/' + orderId + '/status', {
        method: 'PATCH',
        body: JSON.stringify({ managerId, status }),
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось изменить статус');
    }
}

export async function deleteOrder(orderId) {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 150));
        mockOrders = mockOrders.filter((o) => o.id !== orderId);
        return;
    }
    const response = await apiRequest('/orders/' + orderId, {
        method: 'DELETE',
    });
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.detail || 'Не удалось удалить заказ');
    }
}