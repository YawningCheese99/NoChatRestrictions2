# NoChatRestrictions2
A Java agent that removes Minecraft chat restrictions. Version 2.

### How to use: 
add ```-javaagent:/path/to/jar/agent.jar``` to your minecraft command line arguments.
This is compatible with an unmodified client, Fabric, or Forge. Works with all affected Minecraft versions. 

### Differences from version 1: 
* Compatible with all affected Minecraft versions.
* Zero dependencies.
* No bytecode manipulation.

### How it works:
It does a simple binary string search in the binary data of the authentication library class. It searches for a specific url, and replaces it with localhost. A proxy is then started, which filters url paths. Any path that fetches the chat restrictions is returned with an error, and so Minecraft throws an exception and goes to the default behavior, which is to allow all permissions.
