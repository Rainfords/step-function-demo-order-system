import { Order, OrderListResponse, CreateOrderRequest } from './types';

const API_BASE = '/api/orders';

export const api = {
  async getOrders(status?: string, page = 0, size = 20): Promise<OrderListResponse> {
    const params = new URLSearchParams();
    if (status) params.append('status', status);
    params.append('page', page.toString());
    params.append('size', size.toString());

    const response = await fetch(`${API_BASE}?${params}`);
    if (!response.ok) throw new Error('Failed to fetch orders');
    return response.json();
  },

  async getOrder(orderId: string): Promise<Order> {
    const response = await fetch(`${API_BASE}/${orderId}`);
    if (!response.ok) throw new Error('Failed to fetch order');
    return response.json();
  },

  async createOrder(request: CreateOrderRequest): Promise<Order> {
    const response = await fetch(API_BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) throw new Error('Failed to create order');
    return response.json();
  },

  async cancelOrder(orderId: string): Promise<void> {
    const response = await fetch(`${API_BASE}/${orderId}/cancel`, {
      method: 'POST',
    });
    if (!response.ok) throw new Error('Failed to cancel order');
  },

  async getHealth() {
    const response = await fetch(`${API_BASE}/health`);
    return response.ok;
  },
};
