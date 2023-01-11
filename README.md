# Cerebrum

The goal is to develop a lightweight protocol which enables application/ micro-service to
- Easily publish events as they are occurring
- Subscribe for events and receive the events when they occur (without polling for events)

The protocol should guarantee **100% delivery** of all the event messages to all the intended subscribers and absolutely **0 occurrences of message duplicity**.

The project is divided into 2 parts: 

1. **Cerebrum** - Central reactive non-blocking event broker. As the name suggests, cerebrum behaves as the brain in the application network. It is responsible for receiving the event messages published across the network & delivering these event messages to appropriate subscribers in the network.

2. **Synapses** - Applications/ micro-services in the application ecosystem which publish and subscribe events.

## Installation

**Build commands:**
```
mvn install (on root directory)
```
**Execute commands:**
```
mvn spring-boot:run -pl cerebrum (on root directory)

mvn spring-boot:run -pl synaptic-node-1 (on root directory)

mvn spring-boot:run -pl synaptic-node-2 (on root directory)
```
**Execute commands (with SSL debug log option):**
```
mvn spring-boot:run -pl cerebrum -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"

mvn spring-boot:run -pl synaptic-node-1 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"

mvn spring-boot:run -pl synaptic-node-2 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"
```

**Execute commands (with SSL debug log & dynamic debugging options):**
```
mvn spring-boot:run -pl cerebrum -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

mvn spring-boot:run -pl synaptic-node-1 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001"

mvn spring-boot:run -pl synaptic-node-2 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8002"
```

**Execute commands (dynamic debugging options):**
```
mvn spring-boot:run -pl cerebrum -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

mvn spring-boot:run -pl synaptic-node-1 -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001"

mvn spring-boot:run -pl synaptic-node-2 -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8002"
```


## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

## License

[MIT](https://choosealicense.com/licenses/mit/)
