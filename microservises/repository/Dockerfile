FROM openjdk:8-jre-alpine
COPY out/repository-1.0-SNAPSHOT.jar repository-1.0-SNAPSHOT.jar
COPY wait-for-orchestrator.sh wait-for-orchestrator.sh
CMD ["java","-jar","repository-1.0-SNAPSHOT.jar"]