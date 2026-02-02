# NoChatRestrictions2
A Java agent that removes Minecraft chat restrictions. Version 2.

### How to use: 
Add ```-javaagent:/path/to/jar/agent.jar``` to your Minecraft command line arguments.
This is compatible with an unmodified client, Fabric, or Forge. Works with all affected Minecraft versions. 

### Differences from version 1: 
* Compatible with all affected Minecraft versions.
* Zero dependencies.
* No bytecode manipulation.

### How it works:
It does a simple binary string search in the binary data of the authentication library class. It searches for the chat verification url and replaces it to a url that points nowhere. When Minecraft sees this, it throws an exception and goes to the default, which allows all permissions.
