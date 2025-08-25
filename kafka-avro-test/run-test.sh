#!/bin/bash

echo "🚀 Building and running Kafka Avro Test Application"
echo "=================================================="

# Check if Kafka is running
echo "📋 Checking if Kafka is running..."
if ! nc -z localhost 9092 2>/dev/null; then
    echo "❌ Kafka is not running on localhost:9092"
    echo "💡 Please start Kafka first using: docker-compose up -d"
    exit 1
fi

echo "✅ Kafka is running"

# Build the project
echo "🔨 Building project with Maven..."
cd "$(dirname "$0")"
mvn clean compile

if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

echo "✅ Build successful"

# Generate Avro classes
echo "📝 Generating Avro classes..."
mvn avro:schema

if [ $? -ne 0 ]; then
    echo "❌ Avro code generation failed"
    exit 1
fi

echo "✅ Avro classes generated"

# Compile again to include generated classes
echo "🔨 Compiling with generated classes..."
mvn compile

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

echo "✅ Compilation successful"

# Run the test
echo "🚀 Running Kafka Avro test..."
mvn exec:java -Dexec.mainClass="com.anz.fastpayment.test.KafkaAvroTest"

echo "✅ Test completed!"
