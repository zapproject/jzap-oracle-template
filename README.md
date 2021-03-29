# Fun Translation Oracle

This is a Zap oracle utilizing the JZap API. The purpose of the oracle is to utilize the [Fun Translation API](https://funtranslations.com/api/) to create interesting translations.

# Usage
In src/main/java/io/github/oracle/template/jzap/Config.json, modify the EndpointSchema.queryList.query such that the string starts with the type of translations wanted (such as "hodor", "doge", or "pirate") followed by a "-". Here is a full [list](https://funtranslations.com/api/#all) of translations available.
Add the text to be translated after the hypen "-". (e.g. "hodor-hold the door").

Refer to hardhat/dispatchFun.js to run with [hardhat](https://github.com/zapproject/zap-hardhat/tree/feature/typescript-oracle-dispatch)
- Copy over js file into task folder
- Modify hardhat.config.ts to import the task

** The Fun Translations API is being utilized as a public account. There is a limit of 5 API calls per hour.
