import React, { useEffect, useState } from 'react';
import { Order, OrderListResponse } from '../types';
import { api } from '../api';

interface OrderListProps {
  selectedOrderId: string | null;
  onSelectOrder: (orderId: string) => void;
  refreshTrigger: number;
}

export const OrderList: React.FC<OrderListProps> = ({ selectedOrderId, onSelectOrder, refreshTrigger }) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const response: OrderListResponse = await api.getOrders(undefined, 0, 50);
        setOrders(response.orders.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()));
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load orders');
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
    const interval = setInterval(fetchOrders, 5000);
    return () => clearInterval(interval);
  }, [refreshTrigger]);

  if (loading && orders.length === 0) {
    return (
      <div className="card">
        <h2>Recent Orders</h2>
        <div className="loading">
          <div className="spinner"></div> Loading orders...
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <h2>Recent Orders</h2>
        <div className="error">{error}</div>
      </div>
    );
  }

  return (
    <div className="card">
      <h2>Recent Orders ({orders.length})</h2>
      {orders.length === 0 ? (
        <div style={{ color: '#999', padding: '20px 0', textAlign: 'center' }}>
          No orders yet. Create one to get started!
        </div>
      ) : (
        <div>
          {orders.map((order) => (
            <div
              key={order.orderId}
              className={`order-card ${selectedOrderId === order.orderId ? 'selected' : ''}`}
              onClick={() => onSelectOrder(order.orderId)}
            >
              <div className="order-header">
                <span className="order-id">{order.orderId.substring(0, 12)}...</span>
                <span className={`status-badge status-${order.status.toLowerCase()}`}>
                  {order.status}
                </span>
              </div>
              <div className="order-meta">
                <div>
                  <strong>Customer:</strong> {order.customerId}
                </div>
                <div>
                  <strong>Items:</strong> {order.items.length}
                </div>
              </div>
              <div className="order-meta">
                <div>
                  <strong>Total:</strong> ${order.items.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)}
                </div>
                <div>
                  <strong>Created:</strong> {new Date(order.createdAt).toLocaleTimeString()}
                </div>
              </div>
              {order.status === 'FAILED' && order.failureReason && (
                <div className="order-failure-reason">
                  {order.failureReason}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
