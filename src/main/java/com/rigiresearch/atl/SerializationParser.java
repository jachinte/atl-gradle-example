package com.rigiresearch.atl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

/**
 * An parser to serialize and deserialize Ecore models.
 * Users of this class must register the model first (see static block).
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class SerializationParser {

    static {
        EcorePackage.eINSTANCE.eClass();
    }

    /**
     * A params map.
     */
    private final Map<?, ?> params;

    /**
     * Default constructor.
     */
    public SerializationParser() {
        this.params = Collections.EMPTY_MAP;
    }

    /**
     * Serializes an {@link EObject} to a String.
     * Adapted from <a href="https://stackoverflow.com/a/43974978/738968">here
     * </a>.
     * @param eobjects The objects to serialize
     * @return The corresponding XML-formatted string
     * @throws IOException @see Resource#save(OutputStream, Map)
     */
    public String asXml(final EObject... eobjects) throws IOException {
        return this.asXml(Arrays.stream(eobjects).collect(Collectors.toList()));
    }

    /**
     * Serializes an {@link EObject} to a String.
     * Adapted from <a href="https://stackoverflow.com/a/43974978/738968">here
     * </a>.
     * @param eobjects The objects to serialize
     * @return The corresponding XML-formatted string
     * @throws IOException @see Resource#save(OutputStream, Map)
     */
    public String asXml(final List<EObject> eobjects) throws IOException {
        final ResourceSet set = new ResourceSetImpl();
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("xmi", new XMIResourceFactoryImpl());
        // Since we are creating a String, not actually persisting to a file,
        // we will use a "dummy" URI to make sure it uses the correct extension
        final Resource resource = set.createResource(
            URI.createURI("resource.xmi")
        );
        eobjects.stream().forEach(
            object -> resource.getContents().add(object)
        );
        resource.save(stream, this.params);
        return new String(
            stream.toByteArray(),
            StandardCharsets.UTF_8
        );
    }

    /**
     * Loads an {@link EObject} from the given XML representation.
     * @param xml The  XML-formatted string
     * @return The corresponding list of eObjects
     * @throws IOException If something fails while loading the file
     */
    public EList<EObject> asEObjects(final String xml)
        throws IOException {
        final XMIResource resource = new XMIResourceImpl();
        final URIConverter.ReadableInputStream stream =
            new URIConverter.ReadableInputStream(new StringReader(xml));
        resource.load(stream, this.params);
        return resource.getContents();
    }

}
