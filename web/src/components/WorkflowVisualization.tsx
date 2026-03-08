import React from 'react';
import { OrderStatus } from '../types';

interface WorkflowVisualizationProps {
  status: OrderStatus;
}

const WORKFLOW_STEPS: OrderStatus[] = [
  'VALIDATING',
  'RESERVED',
  'PAID',
  'FULFILLED',
  'COMPLETED',
];

const STEP_LABELS: Record<OrderStatus, string> = {
  VALIDATING: 'Validate',
  RESERVED: 'Reserve',
  PAID: 'Pay',
  FULFILLED: 'Fulfill',
  COMPLETED: 'Complete',
  PENDING: 'Pending',
  CANCELLED: 'Cancelled',
  FAILED: 'Failed',
};

export const WorkflowVisualization: React.FC<WorkflowVisualizationProps> = ({ status }) => {
  const getStepStatus = (step: OrderStatus): 'pending' | 'active' | 'completed' | 'failed' => {
    if (status === 'FAILED' || status === 'CANCELLED') {
      return 'failed';
    }

    if (status === 'COMPLETED') {
      return 'completed';
    }

    const currentIndex = WORKFLOW_STEPS.indexOf(status);
    const stepIndex = WORKFLOW_STEPS.indexOf(step);

    if (stepIndex < currentIndex) {
      return 'completed';
    } else if (stepIndex === currentIndex) {
      return 'active';
    } else {
      return 'pending';
    }
  };

  return (
    <div className="workflow-pipeline">
      {WORKFLOW_STEPS.map((step, index) => (
        <React.Fragment key={step}>
          <div className={`workflow-step ${getStepStatus(step)}`}>
            <div>{STEP_LABELS[step]}</div>
            {getStepStatus(step) === 'completed' && <div style={{ fontSize: '1.2em' }}>✓</div>}
            {getStepStatus(step) === 'active' && <div className="spinner"></div>}
          </div>
          {index < WORKFLOW_STEPS.length - 1 && <div className="workflow-arrow">→</div>}
        </React.Fragment>
      ))}
    </div>
  );
};
