#!/bin/bash

set -e

echo "Setting up LocalStack Step Functions..."

# Create IAM role
echo "Creating IAM role..."
docker compose exec -T localstack awslocal iam create-role \
  --role-name StepFunctionsRole \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"states.amazonaws.com"},"Action":"sts:AssumeRole"}]}' \
  2>/dev/null || echo "✓ Role exists"

# Create activities
echo "Creating activities..."
for activity in ValidateOrderActivity ReserveInventoryActivity ProcessPaymentActivity FulfillOrderActivity; do
  docker compose exec -T localstack awslocal stepfunctions create-activity \
    --name "$activity" \
    --region us-east-1 \
    2>/dev/null || echo "✓ $activity exists"
done

# Create state machine
echo "Creating state machine..."
DEFINITION=$(cat ./stepfunctions/order-workflow.json)

docker compose exec -T localstack awslocal stepfunctions create-state-machine \
  --name OrderProcessingWorkflow \
  --definition "$DEFINITION" \
  --role-arn arn:aws:iam::000000000000:role/StepFunctionsRole \
  --region us-east-1 \
  2>/dev/null || echo "✓ State machine exists"

echo ""
echo "✓ LocalStack setup complete!"
echo ""
echo "Verifying state machines:"
docker compose exec -T localstack awslocal stepfunctions list-state-machines --region us-east-1
