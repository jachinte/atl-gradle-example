package com.rigiresearch;

import com.rigiresearch.atl.AtlTransformation;
import static com.rigiresearch.atl.AtlTransformation.ModelType;
import com.rigiresearch.atl.SerializationParser;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.m2m.atl.emftvm.Model;

/**
 * The main class.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
public final class Application {

    /**
     * The main entry point.
     * @param args The application arguments
     */
    public static void main(final String... args) throws IOException {
        new Application().start();
    }

    /**
     * Runs the ALT transformation.
     * @throws IOException If something bad happens while saving the model
     */
    public void start() throws IOException {
        final Map<String, Model> result = new AtlTransformation.Builder()
            .withMetamodel("Simple", "metamodels/Simple.ecore")
            .withMetamodel("Composed", "metamodels/Composed.ecore")
            // There can be several inputs and outputs
            .withModel(ModelType.INPUT, "IN", "models/composed.xmi")
            .withModel(ModelType.OUTPUT, "OUT", "models/simple.xmi")
            .withTransformation("transformations/Composed2Simple.atl")
            .build()
            .run();

        // Write the result to a file (models/simple.xmi)
        result.get("OUT")
            .getResource()
            .save(Collections.EMPTY_MAP);

        // Print out the result to the console
        final EObject simple = result.get("OUT")
            .getResource()
            .getContents()
            .get(0);
        System.out.println(new SerializationParser().asXml(simple));
    }

}
