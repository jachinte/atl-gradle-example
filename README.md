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
	.withModel(ModelType.INPUT, "IN", "models/composed.xmi")
	.withModel(ModelType.OUTPUT, "OUT", "models/simple.xmi")
	.withTransformation("transformations/Composed2Simple.atl")
	.build()
	.run();
```

Read methods in class [AtlTransformation.Builder](src/main/java/com/rigiresearch/atl/AtlTransformation.java) for more options.
