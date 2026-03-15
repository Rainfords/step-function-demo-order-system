import { useEffect } from 'react';
import SockJS from 'sockjs-client';
import * as Stomp from 'stompjs';
import { OrderStatus } from '../types';

interface OrderStatusUpdate {
  orderId: string;
  status: OrderStatus;
  timestamp: string;
}

interface Message {
  body: string;
}

export const useOrderWebSocket = (
  orderId: string | null,
  onStatusUpdate: (status: OrderStatus) => void
) => {
  useEffect(() => {
    if (!orderId) return;

    let stompClient: Stomp.Client | null = null;

    const connect = () => {
      // SockJS needs HTTP/HTTPS URLs, not ws://
      const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
      const wsUrl = `${protocol}//${window.location.hostname}:8080/ws/orders`;
      const socket = new SockJS(wsUrl);
      stompClient = Stomp.over(socket as unknown as WebSocket);

      stompClient.connect(
        {},
        () => {
          // Connection successful
          stompClient!.subscribe(
            `/topic/orders/${orderId}/status`,
            (message: Message) => {
              try {
                const update: OrderStatusUpdate = JSON.parse(message.body);
                onStatusUpdate(update.status);
              } catch (e) {
                console.error('Failed to parse status update:', e);
              }
            }
          );
        },
        (error: unknown) => {
          console.error('WebSocket connection error:', error);
        }
      );
    };

    // Small delay to ensure backend is ready
    const timer = setTimeout(connect, 500);

    return () => {
      clearTimeout(timer);
      if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {});
      }
    };
  }, [orderId, onStatusUpdate]);
};
