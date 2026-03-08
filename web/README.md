# Order Processing UI

A modern React + TypeScript web frontend for visualizing and managing orders in the order processing system.

## Features

✨ **Real-time Order Tracking**
- Create new orders with intuitive form
- View order status and workflow progress
- Auto-refreshing order list and details

🎨 **Workflow Visualization**
- Visual pipeline showing order progression through steps:
  - Validate Order
  - Reserve Inventory
  - Process Payment
  - Fulfill Order
  - Complete

📊 **Order Management**
- View all orders with filtering
- Detailed order information
- Cancel orders in progress
- Track items, payment, and execution details

⚡ **Real-time Updates**
- Auto-refresh every 2-3 seconds
- Manual refresh controls
- Visual indicators for active, completed, and failed steps

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Lightning-fast build tool
- **CSS** - Beautiful, responsive styling

## Setup

### Prerequisites

- Node.js 16+ and npm/yarn
- Backend running on http://localhost:8080

### Installation

```bash
cd web
npm install
```

### Development

Start the development server:

```bash
npm run dev
```

The app will open at `http://localhost:5173` with hot module reloading.

The Vite dev server is configured to proxy API requests to the backend:
- `/api/*` → `http://localhost:8080/api/*`

### Build

```bash
npm run build
```

Generates optimized production build in `dist/` folder.

Preview the build:

```bash
npm run preview
```

## Project Structure

```
src/
├── main.tsx              # React entry point
├── App.tsx              # Main application component
├── types.ts             # TypeScript type definitions
├── api.ts               # API client functions
├── index.css            # Global styles
└── components/
    ├── OrderForm.tsx         # Create new orders
    ├── OrderList.tsx         # Display orders
    ├── OrderDetail.tsx       # Order details & workflow
    └── WorkflowVisualization.tsx # Pipeline visualizer
```

## Key Components

### OrderForm
- Create new orders
- Add/remove items
- Configure payment method
- Form validation

### OrderList
- Display recent orders
- Sort by creation time
- Real-time status updates
- Click to view details

### OrderDetail
- Full order information
- Workflow progress visualization
- Item breakdown with totals
- Payment and execution details
- Cancel functionality
- Auto-refresh toggle

### WorkflowVisualization
- Visual pipeline of order steps
- Shows completed, active, pending steps
- Displays checkmarks for completed steps
- Spinner for active step
- All steps show as completed when order reaches COMPLETED status

## API Integration

The frontend communicates with the Spring Boot backend via REST API:

```
POST   /api/orders                    # Create order
GET    /api/orders                    # List orders
GET    /api/orders/{orderId}          # Get order details
POST   /api/orders/{orderId}/cancel   # Cancel order
```

All API calls are handled through `src/api.ts` with proper error handling.

## Styling

The app uses a modern, responsive design with:
- Gradient header with purple theme
- Card-based layout
- Status badges with color coding
- Workflow pipeline visualization
- Responsive grid (2 columns on desktop, 1 on mobile)
- Smooth animations and transitions

Color scheme:
- **Primary:** #667eea (Purple)
- **Success:** #4caf50 (Green)
- **Warning:** #f57c00 (Orange)
- **Error:** #f44336 (Red)

## Performance Optimizations

- Component memoization where appropriate
- Efficient re-render management
- Debounced API calls
- CSS transitions for smooth animations
- Lazy loading ready

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Modern browsers with ES2020 support

## Development Tips

### Hot Module Replacement (HMR)
Changes to components are reflected instantly without page reload.

### TypeScript
Strict mode enabled for type safety. Check types:
```bash
npx tsc --noEmit
```

### Debugging
- React DevTools browser extension recommended
- Network tab shows API calls
- Console logs for debugging

## Deployment

### Docker

Create a `Dockerfile`:
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Static Hosting

The `dist/` folder can be deployed to:
- Vercel
- Netlify
- AWS S3 + CloudFront
- GitHub Pages
- Any static web host

## Troubleshooting

### Backend connection error
- Ensure backend is running on http://localhost:8080
- Check CORS is enabled in Spring Boot
- Verify API endpoints are accessible

### Orders not refreshing
- Check browser console for API errors
- Verify backend is responding
- Try manual refresh

### Build errors
- Clear `node_modules` and reinstall: `npm install`
- Check Node.js version: `node --version`
- Try `npm run build` directly

## Next Steps

Potential enhancements:
- [ ] Add order search and advanced filtering
- [ ] Order export to CSV/PDF
- [ ] WebSocket for real-time updates instead of polling
- [ ] Dark mode toggle
- [ ] Order analytics dashboard
- [ ] Admin panel for order management
- [ ] Mobile app with React Native

## License

Same as parent project
