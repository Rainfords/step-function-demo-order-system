#!/bin/bash

set -e

echo "Setting up LocalStack Step Functions..."

# Create IAM role
echo "Creating IAM role..."
awslocal iam create-role \
  --role-name StepFunctionsRole \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"states.amazonaws.com"},"Action":"sts:AssumeRole"}]}' \
  > /dev/null

# Create activities
echo "Creating activities..."
for activity in ValidateOrderActivity ReserveInventoryActivity ProcessPaymentActivity FulfillOrderActivity ReleaseInventoryActivity; do
  awslocal stepfunctions create-activity \
    --name "$activity" \
    --region us-east-1 \
    > /dev/null
    echo "Activity $activity created"
done

# Create state machine
echo "Creating state machine..."
DEFINITION=$(cat /tmp/order-workflow.json)

awslocal stepfunctions create-state-machine \
  --name OrderProcessingWorkflow \
  --definition "$DEFINITION" \
  --role-arn arn:aws:iam::000000000000:role/StepFunctionsRole \
  --region us-east-1 \
  > /dev/null

echo ""
echo "✓ LocalStack setup complete!"
echo ""
echo "Verifying state machines:"
awslocal stepfunctions list-state-machines --region us-east-1
