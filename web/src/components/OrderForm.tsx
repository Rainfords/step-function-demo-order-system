import React, { useState } from 'react';
import { CreateOrderRequest, OrderItem } from '../types';
import { api } from '../api';

interface OrderFormProps {
  onOrderCreated: () => void;
}

const SAMPLE_PRODUCTS = [
  { id: 'PROD-001', name: 'Widget A' },
  { id: 'PROD-002', name: 'Widget B' },
  { id: 'PROD-123', name: 'Premium Widget' },
];

export const OrderForm: React.FC<OrderFormProps> = ({ onOrderCreated }) => {
  const [customerId, setCustomerId] = useState('CUST-001');
  const [items, setItems] = useState<OrderItem[]>([{ productId: 'PROD-123', quantity: 1, price: 29.99 }]);
  const [last4, setLast4] = useState('4242');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleAddItem = () => {
    setItems([...items, { productId: 'PROD-123', quantity: 1, price: 29.99 }]);
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof OrderItem, value: string | number) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: typeof newItems[index][field] === 'number' ? Number(value) : value };
    setItems(newItems);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      const request: CreateOrderRequest = {
        customerId,
        items,
        paymentMethod: {
          type: 'CREDIT_CARD',
          last4,
        },
      };

      await api.createOrder(request);
      setSuccess(true);
      setCustomerId('CUST-001');
      setItems([{ productId: 'PROD-123', quantity: 1, price: 29.99 }]);
      setLast4('4242');
      setTimeout(() => setSuccess(false), 3000);
      onOrderCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create order');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card">
      <h2>Create New Order</h2>
      {error && <div className="error">{error}</div>}
      {success && <div className="success">✓ Order created successfully!</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="customerId">Customer ID</label>
          <input
            id="customerId"
            type="text"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label>Items</label>
          <div className="items-container">
            {items.map((item, index) => (
              <div key={index} className="item-input">
                <select
                  value={item.productId}
                  onChange={(e) => handleItemChange(index, 'productId', e.target.value)}
                >
                  {SAMPLE_PRODUCTS.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
                <input
                  type="number"
                  placeholder="Qty"
                  min="1"
                  value={item.quantity}
                  onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
                />
                <input
                  type="number"
                  placeholder="Price"
                  step="0.01"
                  value={item.price}
                  onChange={(e) => handleItemChange(index, 'price', e.target.value)}
                />
                {items.length > 1 && (
                  <button
                    type="button"
                    className="btn btn-danger btn-small"
                    onClick={() => handleRemoveItem(index)}
                    style={{ width: 'auto' }}
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleAddItem}
            style={{ marginBottom: '15px' }}
          >
            + Add Item
          </button>
        </div>

        <div className="form-group">
          <label htmlFor="last4">Card Last 4 Digits</label>
          <input
            id="last4"
            type="text"
            maxLength={4}
            value={last4}
            onChange={(e) => setLast4(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Creating...' : 'Create Order'}
        </button>
      </form>
    </div>
  );
};
