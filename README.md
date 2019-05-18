# ATL gradle example

An object-oriented gradle-based ATL launcher based on Victor Guana's [ATL Launcher](https://github.com/guana/ATLauncher).

### Run the example

```bash
gradle run
```

### Code

This is how to run a transformation:

```java
final Map<String, Model> result = new AtlTransformation.Builder()
    .withMetamodel("Simple", "metamodels/Simple.ecore")
    .withMetamodel("Composed", "metamodels/Composed.ecore")
    // There can be several inputs and outputs
    .withInput("IN", "models/composed.xmi")
    .withOutput("OUT", "models/simple.xmi")
    .withTransformation("transformations/Composed2Simple.atl")
    .build()
    .run();
```

Read methods in class [AtlTransformation.Builder](src/main/java/com/rigiresearch/atl/AtlTransformation.java) for more options. In particular, there is a method to read a metamodel from a JAR file and a method to specify an input/output model from a EObject.
