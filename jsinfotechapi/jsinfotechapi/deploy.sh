#!/bin/bash

# ===== CONFIG =====
SERVER_USER=root
SERVER_IP=172.236.176.110
REMOTE_DIR=/root
JAR_NAME=springJDBC-1.0-SNAPSHOT.jar
APP_PORT=80

echo "🔨 Building project..."
mvn clean install -DskipTests || { echo "Build failed ❌"; exit 1; }

echo "📦 Copying jar to server..."
scp target/$JAR_NAME ${SERVER_USER}@${SERVER_IP}:${REMOTE_DIR}/ || { echo "SCP failed ❌"; exit 1; }

echo "🛑 Stopping old application..."
ssh ${SERVER_USER}@${SERVER_IP} "
pkill -f $JAR_NAME || true
sleep 3
"

echo "🚀 Starting new application..."
ssh ${SERVER_USER}@${SERVER_IP} "
cd ${REMOTE_DIR}
nohup java -jar $JAR_NAME --server.port=${APP_PORT} > app.log 2>&1 &
sudo firewall-cmd --permanent --add-port=${APP_PORT}/tcp >/dev/null 2>&1
sudo firewall-cmd --reload >/dev/null 2>&1
"

echo "✅ Deployment completed!"
