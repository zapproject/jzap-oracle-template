# JZap oracle-template

## Purpose :
To help users fully creating and running an off-chain Oracle with just couple config and 1 function implementation.
## ORACLE TEMPLATE SETUP EXPLAINED

### Config Setting :
  - Oracle's information :
    + Title, Public key, Account address, Node URL
    + Endpoint : Name, Curve, broker, md (description about endpoint), query list ( query string accepted and response type)
### What will be created  once setup and run:
1. Oracle registered if none exists
2. Endpoint created if Endpoint name in config has not been created
  + Endpoint.json file will be created containing information about query list and endpoint, saved in ipfs and set as Endpoint's params on-chain
  + Endpoint.md file will be created, saved in ipfs and set as Provider's params on-chain
3. If Endpoint is already initiated, the step 2 will be ignored


### Code Layout :

1. Config.ts : data about your wallet ,ethereum node and your provider's pubkey and title
3. Oracle.ts : Template for Create/Manage  flow
4. Responder.ts :  Stub callback function when receive query event and return result

### Usage :

1. Configure Config.ts
2. Implement function `getResponse` in Responder.ts
3. Run `Main.java` to start create/get Oracle and start listening to queries   

## Note :

- Ensure you have enough ETH in your address for responding to queries

############################################

# Inspire Oracle for Containers

A Zap Oracle which provides a few inspirational words.

Features:
- A random app idea (http://itsthisforthat.com/)
- A random advice (https://api.adviceslip.com/#top)
- A random technical quote (http://quotes.stormconsultancy.co.uk/)

This Oracle utilizes the JZap API https://github.com/zapproject/jzap to interact with Zap Contracts.
Run: 
    "mvn clean install"
    "mvn exec:java -Dexec.mainClass="io.github.oracle.template.jzap.Main""

The subscriber functionality is not ran for this version.