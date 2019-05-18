package com.rigiresearch.atl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.BasicExtendedMetaData;
import org.eclipse.emf.ecore.util.ExtendedMetaData;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Metamodel;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.TimingData;

/**
 * An object-oriented ATL/EMFTVM transformation launcher.
 * This class is based on <a href="https://github.com/guana/ATLauncher/">Victor
 * Guana's ATL launcher</a>.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
@RequiredArgsConstructor
@ToString
public final class AtlTransformation {

    /**
     * Pairs of metamodel name and file.
     * <br />For example: (Metamodel, /path/to/Metamodel.ecore).
     */
    private final Map<String, File> metamodels;
    /**
     * Pairs of input variable name and model.
     * <br />For example: (IN, /path/to/Model.xmi).
     */
    private final Map<String, File> inputs;
    /**
     * Pairs of output variable name and model.
     * <br />For example: (OUT, /path/to/Model.xmi).
     */
    private final Map<String, File> outputs;
    /**
     * The transformation module.
     */
    private final File transformation;

    /**
     * Launches the transformation.
     * @return the output models
     */
    public Map<String, Model> run() {
        // Lazy registration first
        Map<File, URI> uris = new HashMap<>();
        for (Map.Entry<String, File> entry : this.metamodels.entrySet()) {
            uris.put(entry.getValue(), this.nsURI(entry.getValue()));
        }
        final ResourceSet set = new ResourceSetImpl();
        ExecEnv environment = EmftvmFactory.eINSTANCE.createExecEnv();
        this.registerMetamodels(uris, set, environment);
        // Create and register resource factories to read/parse .xmi and .emftvm
        // files. The latter corresponds to files created by the transformation
        // compiler (ATL-EMFTV compiler)
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("xmi", new XMIResourceFactoryImpl());
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("emftvm", new EMFTVMResourceFactoryImpl());
        // Register the input and out models
        this.registerModels(set, environment, this.inputs, true);
        final Map<String, Model> models =
            this.registerModels(set, environment, this.outputs, false);
        // Load and run the transformation module
        final String directory =
            String.format("%s/", this.transformation.getParent());
        final String module = this.transformation.getName()
            .substring(0, this.transformation.getName().lastIndexOf("."));
        final DefaultModuleResolver resolver =
            new DefaultModuleResolver(directory, set);
        final TimingData data = new TimingData();
        environment.loadModule(resolver, module);
        data.finishLoading();
        environment.run(data);
        data.finish();
        return models;
    }

    /**
     * Loads the given input/output models in the resource set and makes them
     * available in the execution environment.
     * @param set The resource set
     * @param environment The execution environment
     * @param models The input/output models
     * @param input Whether the models are input or not (i.e., output)
     * @return The registered models
     */
    private Map<String, Model> registerModels(final ResourceSet set,
        final ExecEnv environment, final Map<String, File> models,
        final boolean input) {
        final Map<String, Model> result = new HashMap<>();
        for (Map.Entry<String, File> entry : models.entrySet()) {
            final Model model = EmftvmFactory.eINSTANCE.createModel();
            final URI uri = URI.createURI(entry.getValue().getAbsolutePath());
            if (input) {
                model.setResource(set.getResource(uri, true));
                environment.registerInputModel(entry.getKey(), model);
            } else {
                model.setResource(set.createResource(uri));
                environment.registerOutputModel(entry.getKey(), model);
            }
            result.put(entry.getKey(), model);
        }
        return result;
    }

    /**
     * Loads the metamodels in the resource set and makes them available in the
     * execution environment.
     * @param uris The nsURI for each metamodel
     * @param set The resource set
     * @param environment The execution environment
     */
    private void registerMetamodels(final Map<File, URI> uris,
        final ResourceSet set, final ExecEnv environment) {
        for (Map.Entry<String, File> entry : this.metamodels.entrySet()) {
            final Metamodel mm = EmftvmFactory.eINSTANCE.createMetamodel();
            mm.setResource(set.getResource(uris.get(entry.getValue()), true));
            environment.registerMetaModel(entry.getKey(), mm);
        }
    }

    /**
     * Lazy registration of the metamodel to get the nsURI.
     * @param metamodel The metamodel file to register
     * @return The metamodel's nsURI
     */
    private URI nsURI(File metamodel) {
        URI uri = URI.createURI("");
        Resource.Factory.Registry.INSTANCE
            .getExtensionToFactoryMap()
            .put("ecore", new EcoreResourceFactoryImpl());
        final ResourceSet set = new ResourceSetImpl();
        final ExtendedMetaData md = new BasicExtendedMetaData(
            EPackage.Registry.INSTANCE
        );
        set.getLoadOptions().put(XMLResource.OPTION_EXTENDED_META_DATA, md);
        final Resource resource = set.getResource(
            URI.createFileURI(metamodel.getAbsolutePath()), true
        );
        final EObject eobject = resource.getContents().get(0);
        // A meta-model might have multiple packages we assume the main package
        // is the first one listed
        if (eobject instanceof EPackage) {
            final EPackage epackage = (EPackage) eobject;
            EPackage.Registry.INSTANCE.put(epackage.getNsURI(), epackage);
            uri = URI.createURI(epackage.getNsURI());
        }
        return uri;
    }

    /**
     * An ATL transformation builder.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    public static final class Builder {
        private final Map<String, File> metamodels;
        private final Map<String, File> inputs;
        private final Map<String, File> outputs;
        private File transformation;

        /**
         * Default constructor.
         */
        public Builder() {
            this.metamodels = new HashMap<>();
            this.inputs = new HashMap<>();
            this.outputs = new HashMap<>();
        }

        /**
         * Set the ATL transformation.
         * @param path The path to the .atl file
         * @return This builder
         */
        public Builder withTransformation(final String path) {
            this.transformation = new File(path);
            return this;
        }

        /**
         * Adds a metamodel to the transformation context.
         * <p>This method creates a temporal .ecore file and copies the
         * metamodel from the .jar file.
         * @param name The name of the metamodel
         * @param jar The path to the .jar file containing the metamodel
         * @param metamodel The path to the .ecore metamodel within the jar file
         * @return This builder
         * @throws IOException If something happens creating the temporal file
         */
        public Builder withMetamodelFromJar(final String name,
            final String jar, final String metamodel) throws IOException {
            final File file = File.createTempFile(name, ".ecore");
            final String path = String.format(
                "jar:file:%s!/%s",
                new File(jar).getAbsolutePath(),
                metamodel
            );
            file.getParentFile().mkdir();
            file.createNewFile();
            URL url = new URL(URLDecoder.decode(path, "UTF-8"));
            Files.copy(
                url.openStream(),
                Paths.get(file.toURI()),
                StandardCopyOption.REPLACE_EXISTING
            );
            return this.withMetamodel(name, file.getPath());
        }

        /**
         * Adds a metamodel to the transformation context.
         * @param name The name of the metamodel
         * @param path The path to the metamodel
         * @return This builder
         */
        public Builder withMetamodel(final String name, final String path) {
            this.metamodels.put(name, new File(path));
            return this;
        }

        /**
         * Adds an input model to the transformation context.
         * <p>This method creates a temporal .xmi file from the given model.
         * @param name The name of the ATL variable (e.g., IN)
         * @param model The model
         * @return This builder
         * @throws IOException If something happens while serializing the model
         */
        public Builder withInput(final String name, final EObject model)
            throws IOException {
            return this.withInput(name, this.asFile(model).getPath());
        }

        /**
         * Adds an input model to the transformation context.
         * @param name The name of the ATL variable (e.g., IN)
         * @param path The path to the input model
         * @return This builder
         */
        public Builder withInput(final String name, final String path) {
            this.inputs.put(name, new File(path));
            return this;
        }

        /**
         * Creates a temporal .xmi file from the given model.
         * @param model The model
         * @return The temporal file
         * @throws IOException If something happens while serializing the model
         */
        private File asFile(final EObject model)
            throws IOException {
            final File file = File.createTempFile("model", ".ecore");
            Files.write(
                Paths.get(file.toURI()),
                new SerializationParser().asXml(model).getBytes(),
                StandardOpenOption.TRUNCATE_EXISTING
            );
            return file;
        }

        /**
         * Adds an output model to the transformation context.
         * <p>This method creates a temporal .xmi file from the given model.
         * @param name The name of the ATL variable (e.g., OUT)
         * @param model The model
         * @return This builder
         * @throws IOException If something happens while serializing the model
         */
        public Builder withOutput(final String name, final EObject model)
            throws IOException {
            return this.withOutput(name, this.asFile(model).getPath());
        }

        /**
         * Adds an output model to the transformation context.
         * @param name The name of the ATL variable (e.g., OUT)
         * @param path The path to the output model
         * @return This builder
         */
        public Builder withOutput(final String name, final String path) {
            this.outputs.put(name, new File(path));
            return this;
        }

        /**
         * Builds the transformation.
         * @return A new ATL transformation
         */
        public AtlTransformation build() {
            if (this.transformation == null)
                throw new IllegalArgumentException(
                    "The transformation module is required");
            else if (this.metamodels.isEmpty())
                throw new IllegalArgumentException(
                    "At least one metamodel is required");
            else if (this.inputs.isEmpty())
                throw new IllegalArgumentException(
                    "At least one input model is required");
            else if (this.outputs.isEmpty())
                throw new IllegalArgumentException(
                    "At least one output model is required");
            return new AtlTransformation(
                this.metamodels,
                this.inputs,
                this.outputs,
                this.transformation
            );
        }
    }

}
