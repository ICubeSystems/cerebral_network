# nceph-network
Network of synaptic nodes with a central event relay server (cerebrum)

<b>Build commands:</b>

mvn install (on core directory)

mvn install (on synapse directory)

mvn install (on root directory)

<b>Execute commands:</b>

mvn spring-boot:run -pl cerebrum (on root directory)

mvn spring-boot:run -pl synaptic-node-1 (on root directory)

mvn spring-boot:run -pl synaptic-node-2 (on root directory)

<b>Execute commands (with SSL debug log option):</b>

mvn spring-boot:run -pl cerebrum -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"

mvn spring-boot:run -pl synaptic-node-1 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"

mvn spring-boot:run -pl synaptic-node-2 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all"


<b>Execute commands (with SSL debug log & dynamic debugging options):</b>

mvn spring-boot:run -pl cerebrum -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000"

mvn spring-boot:run -pl synaptic-node-1 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001"

mvn spring-boot:run -pl synaptic-node-2 -Dspring-boot.run.jvmArguments="-Djavax.net.debug=all -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8002"

