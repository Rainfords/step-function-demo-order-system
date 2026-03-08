export type OrderStatus =
  | 'PENDING'
  | 'VALIDATING'
  | 'RESERVED'
  | 'PAID'
  | 'FULFILLED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'FAILED';

export interface OrderItem {
  productId: string;
  quantity: number;
  price: number;
}

export interface PaymentMethod {
  type: string;
  last4: string;
}

export interface Order {
  orderId: string;
  customerId: string;
  items: OrderItem[];
  paymentMethod: PaymentMethod;
  status: OrderStatus;
  executionArn: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  customerId: string;
  items: OrderItem[];
  paymentMethod: PaymentMethod;
}

export interface OrderListResponse {
  orders: Order[];
  total: number;
  page: number;
  size: number;
}
