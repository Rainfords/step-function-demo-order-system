import React, { useState } from 'react';
import { OrderForm } from './components/OrderForm';
import { OrderList } from './components/OrderList';
import { OrderDetail } from './components/OrderDetail';
import { api } from './api';
import './index.css';

function App() {
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleOrderCreated = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  const handleCancelOrder = async () => {
    if (!selectedOrderId) return;

    if (confirm('Are you sure you want to cancel this order?')) {
      try {
        await api.cancelOrder(selectedOrderId);
        setRefreshTrigger((prev) => prev + 1);
      } catch (error) {
        alert('Failed to cancel order: ' + (error instanceof Error ? error.message : 'Unknown error'));
      }
    }
  };

  return (
    <div>
      <header>
        <h1>📦 Order Processing System</h1>
        <p>AWS Step Functions with Spring Boot Backend</p>
      </header>

      <div className="main-content">
        <div>
          <OrderForm onOrderCreated={handleOrderCreated} />
          <OrderList
            selectedOrderId={selectedOrderId}
            onSelectOrder={setSelectedOrderId}
            refreshTrigger={refreshTrigger}
          />
        </div>

        {selectedOrderId ? (
          <OrderDetail
            orderId={selectedOrderId}
            onCancel={handleCancelOrder}
          />
        ) : (
          <div className="card">
            <h3>Select an order to view details</h3>
            <p style={{ color: '#999', marginTop: '10px' }}>
              Create a new order or select an existing one from the list to see its workflow progress and details.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
