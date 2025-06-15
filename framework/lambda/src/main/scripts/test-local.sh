#!/bin/bash

# Build the Lambda package first
echo "Building Lambda package..."
../gradlew shadowJar

# Create deployment zip
echo "Creating deployment package..."
cd build/libs
zip -r ../distributions/chinook-lambda-deployment.zip chinook-lambda.jar
cd ../..

# Start SAM local API
echo "Starting SAM local API..."
echo "API will be available at http://localhost:3000"
echo "Press Ctrl+C to stop"
sam local start-api --warm-containers EAGER