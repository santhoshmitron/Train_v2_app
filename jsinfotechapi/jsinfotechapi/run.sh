#!/bin/bash
# Startup script for Spring Boot application with Java 9+ compatibility
# This script sets the required JVM arguments to open modules for CGLIB

export MAVEN_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"

mvn spring-boot:run


