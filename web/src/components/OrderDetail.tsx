import React, { useEffect, useState, useCallback } from 'react';
import { Order, OrderStatus } from '../types';
import { WorkflowVisualization } from './WorkflowVisualization';
import { api } from '../api';
import { useOrderWebSocket } from '../hooks/useOrderWebSocket';

interface OrderDetailProps {
  orderId: string;
  onCancel?: () => void;
}

export const OrderDetail: React.FC<OrderDetailProps> = ({ orderId, onCancel }) => {
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Load initial order data and periodically sync execution status
  useEffect(() => {
    const fetchOrder = async () => {
      try {
        setLoading(true);
        const data = await api.getOrder(orderId);
        setOrder(data);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load order');
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();

    // Poll periodically to sync execution status with Step Functions
    // This ensures we catch the COMPLETED status when the execution finishes
    const interval = setInterval(fetchOrder, 10000);
    return () => clearInterval(interval);
  }, [orderId]);

  // Handle real-time status updates via WebSocket
  const handleStatusUpdate = useCallback((status: OrderStatus) => {
    setOrder(prev => {
      if (!prev) return prev;
      return { ...prev, status, updatedAt: new Date().toISOString() };
    });
  }, []);

  // Subscribe to WebSocket updates
  useOrderWebSocket(orderId, handleStatusUpdate);

  if (loading) {
    return (
      <div className="order-detail-view">
        <div className="loading">
          <div className="spinner"></div> Loading order details...
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="order-detail-view">
        <div className="error">{error || 'Order not found'}</div>
      </div>
    );
  }

  const total = order.items.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const isCompleted = order.status === 'COMPLETED';
  const isFailed = order.status === 'FAILED';

  return (
    <div className="order-detail-view">
      <div className="order-detail-header">
        <div>
          <h3>Order {order.orderId.substring(0, 8)}...</h3>
          <p style={{ color: '#999', fontSize: '0.9em', marginTop: '5px' }}>{order.orderId}</p>
        </div>
        <div>
          <span className={`status-badge status-${order.status.toLowerCase()}`}>
            {order.status}
          </span>
        </div>
      </div>

      <div className="detail-section">
        <h4>Workflow Progress</h4>
        <WorkflowVisualization status={order.status} />
        {!isCompleted && !isFailed && onCancel && (
          <div style={{ marginTop: '15px', display: 'flex', gap: '10px' }}>
            <button
              className="btn btn-danger btn-small"
              onClick={onCancel}
            >
              Cancel Order
            </button>
          </div>
        )}
      </div>

      <div className="detail-section">
        <h4>Order Information</h4>
        <div className="detail-row">
          <span className="detail-label">Customer ID:</span>
          <span className="detail-value">{order.customerId}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Created:</span>
          <span className="detail-value">{new Date(order.createdAt).toLocaleString()}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Last Updated:</span>
          <span className="detail-value">{new Date(order.updatedAt).toLocaleString()}</span>
        </div>
      </div>

      <div className="detail-section">
        <h4>Items ({order.items.length})</h4>
        <ul className="items-list">
          {order.items.map((item, index) => (
            <li key={index}>
              <span>{item.productId}</span>
              <span style={{ textAlign: 'center' }}>Qty: {item.quantity}</span>
              <span style={{ textAlign: 'right' }}>${(item.price * item.quantity).toFixed(2)}</span>
            </li>
          ))}
        </ul>
        <div
          style={{
            marginTop: '10px',
            paddingTop: '10px',
            borderTop: '2px solid #e0e0e0',
            textAlign: 'right',
            fontWeight: 'bold',
          }}
        >
          Total: ${total.toFixed(2)}
        </div>
      </div>

      <div className="detail-section">
        <h4>Payment Method</h4>
        <div className="detail-row">
          <span className="detail-label">Type:</span>
          <span className="detail-value">{order.paymentMethod.type}</span>
        </div>
        <div className="detail-row">
          <span className="detail-label">Card:</span>
          <span className="detail-value">•••• {order.paymentMethod.last4}</span>
        </div>
      </div>

      {order.executionArn && (
        <div className="detail-section">
          <h4>Execution Details</h4>
          <div className="detail-row">
            <span className="detail-label">ARN:</span>
            <span className="detail-value" style={{ fontSize: '0.8em' }}>{order.executionArn}</span>
          </div>
        </div>
      )}
    </div>
  );
};
