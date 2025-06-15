#!/bin/bash

echo "🚀 Simple Chinook Lambda Deployment"
echo "=================================="

# Check if AWS CLI is configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS CLI not configured. Run 'aws configure' first"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region)

echo "✓ Using AWS Account: $ACCOUNT_ID"
echo "✓ Region: $REGION"

# Step 1: Build the Lambda JAR
echo -e "\n📦 Building Lambda package..."
./gradlew :chinook-server-aws-lambda:shadowJar
if [ ! -f "build/libs/chinook-lambda.jar" ]; then
    echo "❌ Build failed!"
    exit 1
fi
echo "✓ Build successful"

# Step 2: Create simple IAM role (if it doesn't exist)
echo -e "\n🔐 Setting up IAM role..."
if ! aws iam get-role --role-name chinook-simple-lambda-role &> /dev/null; then
    aws iam create-role \
        --role-name chinook-simple-lambda-role \
        --assume-role-policy-document '{
            "Version": "2012-10-17",
            "Statement": [{
                "Effect": "Allow",
                "Principal": {"Service": "lambda.amazonaws.com"},
                "Action": "sts:AssumeRole"
            }]
        }' > /dev/null
    
    # Attach basic execution policy
    aws iam attach-role-policy \
        --role-name chinook-simple-lambda-role \
        --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
    
    echo "✓ IAM role created"
    sleep 10  # Give AWS time to propagate the role
else
    echo "✓ IAM role already exists"
fi

# Step 3: Create or update Lambda function
echo -e "\n☁️  Deploying Lambda function..."
if aws lambda get-function --function-name chinook-server &> /dev/null; then
    # Update existing function
    aws lambda update-function-code \
        --function-name chinook-server \
        --zip-file fileb://build/libs/chinook-lambda.jar > /dev/null
    echo "✓ Lambda function updated"
else
    # Create new function
    aws lambda create-function \
        --function-name chinook-server \
        --runtime java21 \
        --role arn:aws:iam::$ACCOUNT_ID:role/chinook-simple-lambda-role \
        --handler is.codion.demos.chinook.lambda.EntityProtocolHandler::handleRequest \
        --zip-file fileb://build/libs/chinook-lambda.jar \
        --timeout 30 \
        --memory-size 1024 \
        --environment Variables="{DATABASE_URL=jdbc:h2:mem:chinook,DATABASE_USER=scott,DATABASE_PASSWORD=tiger}" > /dev/null
    echo "✓ Lambda function created"
fi

# Step 4: Create Function URL (simpler than API Gateway)
echo -e "\n🌐 Creating public endpoint..."
if ! aws lambda get-function-url-config --function-name chinook-server &> /dev/null 2>&1; then
    aws lambda create-function-url-config \
        --function-name chinook-server \
        --auth-type NONE > /dev/null
fi

# Add permissions
aws lambda add-permission \
    --function-name chinook-server \
    --statement-id FunctionURLAllowPublicAccess \
    --action lambda:InvokeFunctionUrl \
    --principal "*" \
    --function-url-auth-type NONE &> /dev/null 2>&1

# Get the URL
FUNCTION_URL=$(aws lambda get-function-url-config \
    --function-name chinook-server \
    --query FunctionUrl \
    --output text)

echo "✓ Public endpoint created"

# Step 5: Test it
echo -e "\n🧪 Testing deployment..."
echo "Testing health endpoint..."
curl -s -X GET "${FUNCTION_URL}health" | jq . || echo "Response: $(curl -s -X GET "${FUNCTION_URL}health")"

echo -e "\n✅ Deployment complete!"
echo "========================================"
echo "Your Chinook Lambda URL is:"
echo "$FUNCTION_URL"
echo ""
echo "To use with HttpEntityConnection:"
echo "  .baseUrl(\"${FUNCTION_URL%/}\")"
echo "  .https(true)"
echo ""
echo "Note: Using in-memory H2 database (data won't persist between cold starts)"
echo "========================================"