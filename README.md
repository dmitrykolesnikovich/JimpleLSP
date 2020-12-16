# JimpleLSP
This is an implementation of the Language Server Protocol for Jimple.
It allows you to use well known features of your IDE to be used while exploring and editing .jimple files.

# Usage
## IntelliJ Idea
1. Install or use an LSP Plugin in your IDE to enable Language Server Protocoll Support for your IDE.
e.g. for IntelliJ you can use https://github.com/MagpieBridge/IntelliJLSP/tree/intellijclientlib
2.
    a)  **Either** Download the JimpleLSP Server Release and extract it to any Location.
  
    b) **OR** Compile it yourself 
   You need a fresh build of FuturSoot in your local Maven Repository.
   To get it run `git clone https://github.com/secure-software-engineering/soot-reloaded.git && cd soot-reloaded && mvn install`
   (Latest working commit is #6bb957a82d28062f74586d4333da90172db48f18).
   
    Then run
    `git clone https://github.com/swissiety/JimpleLSP` to clone this Repository.
    Build the Jar via `mvn package`. The generated Jar can be found in ./target/jimplelsp-0.0.1-SNAPSHOT-jar-with-dependencies.jar.
    
4. Configure the LSP Plugin in your IDE to use the JimpleLSP Server Jar that was extracted or build in the previous step.
5. Enjoy!


## VSCode
Assumes: npm is installed
1. `cd vscode/ && npm install` to install the dependencies.
2. `npm install -g vsce` to install the Visual Studio Code Extension Commandline tool. 
3. `vsce package` to package the Plugin.
4. `code --install-extension JimpleLSP-0.0.1.vsix` 
5. restart VSCode

## Server Capabilities
This Server is oriented towards LSP 3.15. The currently implemented features are focussed on improving code exploring in Jimple.

### Text Document Capabilities
- ✅ didOpen
- ❌ [planned #17] didChange
    - ❌ Full text sync
    - ❌ Incremental text sync
- ✅ didSave
    - ✅ Include text
- ❌ completion
- ✅ hover
- ❌ signatureHelp
- ✅ declaration
    - ❌ [planned #18] link support
- ✅ definition
    - ❌ [planned #18] link support
- ✅ typeDefinition
    - ❌ [planned #18] link support
- ✅ implementation
    - ❌ [planned #18] link support
- ✅ references
- ❌ [planned #11] documentHighlight
- ✅ documentSymbol
- ❌ codeAction
    - ❌ resolve
- ❌ codeLens
- ❌ documentLink
- ❌ formatting
- ❌ rangeFormatting
- ❌ onTypeFormatting
- ❌ rename
- ✅ publishDiagnostics
- ❌ [WIP #16] foldingRange
- ❌ selectionRange
- ❌ [planned #1] semanticToken
- ❌ callHierarchy

### Workspace Capabilities
- ❌ [planned #9] applyEdit
- ❌ didChangeConfiguration
- ❌ [planned #17] didChangeWatchedFiles
- ✅ symbol
- ❌ executeCommand

### Window Capabilities

- ❌ workDoneProgress
    - ❌ create
    - ❌ cancel
- ✅ logMessage
- ✅ showMessage
- ✅ showMessage request


## About
This piece of software was created as part of a bachelor thesis at UPB (University of Paderborn, Germany).