FROM openjdk:12-jdk-alpine
COPY out/block-1.0-SNAPSHOT.jar block-1.0-SNAPSHOT.jar
COPY wait-for-orchestrator.sh wait-for-orchestrator.sh
CMD ["java","-jar","block-1.0-SNAPSHOT.jar"]