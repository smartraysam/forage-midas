# Midas
Project repo for the JPMC Advanced Software Engineering Forage program.

## Project setup task 1
## What you'll learn
- How Software Engineers build and configure backend systems used to process high-volume financial transactions.
- How to set up a Java development environment using Java 17, Spring Boot, Maven, and an IDE that supports enterprise projects.
- How to work with engineering requirements and prepare a project scaffold for future integration tasks.

## What you'll do
- Set up your local development environment by installing Java 17, forking and cloning the project repository, and opening it in your IDE.
- Explore the existing project scaffold to understand how the Midas Core service is structured.
- Add the required dependencies to your Spring Boot project and update configuration files.
- Build and run the project, then verify your setup by running automated tests.


## Integrate kafka
## What you'll learn
- How message queues—specifically Kafka—are used to decouple services, improve scalability, and enable asynchronous communication in large backend systems.
- How to integrate Kafka into a Spring Boot application by creating a listener that consumes messages from a configured topic.
- How to deserialize incoming Kafka messages into domain objects and verify your integration using an embedded Kafka instance and automated tests.

## What you'll do
- Implement a Kafka listener in Midas Core that reads from the topic defined in application.yml and deserializes each incoming message into the provided Transaction class.
- Configure your listener to use the project’s existing Spring Boot setup—no need to specify host/port, as the tests use an embedded Kafka instance.
- Run TaskTwoTests, use your debugger to inspect the first four received transactions, and record the amounts attached to each.

## Integrate 