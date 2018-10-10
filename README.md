# fileSyncr

WIP app that allows working on multiple text files from multiple computers at the same time.

### Still left to do (not necessarily in this order):

* replace the Java serializer with something more efficient. Getting warning:
```Using the default Java serializer for class [actors.Messages$EventDataMessage$DiffEventMsg] which is not recommended because of performance implications. Use another serializer or disable this warning using the setting 'akka.actor.warn-about-java-serializer-usage'```

* create some simple ui (cli) and test:
    1. locally
    2. one local and one remote

* create some real ui (Vaadin maybe? JavaFX?)

* move to ssl

* add some authentication method

* correct info logs to debug

* add messages for syncing
    1. resetting
        1. delete existing files
        1. create all the files
        2. set content to all files
        
    2. rejoining
        1. create missing files
        2. delete excessive files
        3. update file content
        
        
 