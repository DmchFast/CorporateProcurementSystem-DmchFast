import { apiRequest } from './client';

const USE_MOCKS = true;

let mockCategories = [
    { id: '2001', name: 'Офисная техника' },
    { id: '2002', name: 'Канцелярские товары' },
    { id: '2003', name: 'Мебель' },
    { id: '2004', name: 'Расходные материалы' },
];

export async function getCategories() {
    if (USE_MOCKS) {
        await new Promise((r) => setTimeout(r, 80));
        return mockCategories.slice();
    }
    const response = await apiRequest('/categories');
    if (!response.ok) {
        throw new Error('Не удалось загрузить категории');
    }
    return response.json();
}