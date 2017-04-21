# embeddedneo_ha
### Neo4j Embedded in HA mode - `ext_config` branch. The objective is demonstration of how to run HA cluster with one embedded and one standalone instance

- uses external config file (passed as a parameter) to load graphdb configs
- sample config files : `neo4j1.conf, neo4j.conf` can be used to launch a 2-instance HA cluster

## Setup steps:
1. Clone the repo, switch to `ext_config` branch
2. download and install Neo4j Enterprise (I'm using 3.1.1)
3. replace `neo4j.conf` file in the standalone instance with `neo4j.conf` from this repo
4. `mvn clean package` against the pom.xml
5. launch the embedded instance: `mvn exec:java -Dexec.mainClass="com.rgabay.embedded_ha.Driver" -Dexec.args="-c neo4j1.conf"` 
6. launch the standalone instance - `neo4j start` or `neo4j console` from the `bin` directory - it will expose port `7475` for UI and `7690` for the Bolt protocol. 

Once the instances come up you should have a 2 instance HA cluster. Navigate to `localhost:7475`, run `:sysinfo` to check cluster status.

When the embedded instance joins the cluster it creates 2 `Airport` nodes and an index on the `code` property. To verify - run `match (n) return (n) limit 5` and `call db.schema()` from the UI. 

You can also change the bolt URL in the UI to point at `localhost:7688` to hit the embedded instance directly.

