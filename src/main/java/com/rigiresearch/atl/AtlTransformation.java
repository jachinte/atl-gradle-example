package com.rigiresearch.atl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
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
@SuppressWarnings("ClassDataAbstractionCoupling")
public final class AtlTransformation {

    /**
     * Pairs of metamodel name and nsURI.
     */
    private final Map<String, URI> metamodels;

    /**
     * A map of models organized by type.
     */
    private final Map<ModelType, List<NamedModel>> models;

    /**
     * The transformation module.
     */
    private final File transformation;

    /**
     * Launches the transformation.
     * @return The output models
     */
    public Map<String, Model> run() {
        final ResourceSet set = new ResourceSetImpl();
        final ExecEnv environment = EmftvmFactory.eINSTANCE.createExecEnv();
        this.registerMetamodels(set, environment);
        this.registerFactories(set);
        // Register the models
        final Map<String, Model> instances =
            this.registerModels(set, environment, this.models);
        // Load and run the transformation module
        final TimingData data = new TimingData();
        final DefaultModuleResolver resolver =
            new DefaultModuleResolver(this.moduleDirectory(), set);
        environment.loadModule(resolver, this.moduleName());
        data.finishLoading();
        environment.run(data);
        data.finish();
        return instances;
    }

    /**
     * The ATL module directory.
     * @return The module directory path
     */
    private String moduleDirectory() {
        return String.format("%s/", this.transformation.getParent());
    }

    /**
     * The ATL module name.
     * @return The module name without extension
     */
    private String moduleName() {
        return this.transformation.getName()
            .substring(0, this.transformation.getName().lastIndexOf("."));
    }

    /**
     * Loads the given input/output models in the resource set and makes them
     * available in the execution environment.
     * @param set The resource set
     * @param environment The execution environment
     * @param config The input, output and in-out models
     * @return The registered models
     */
    @SuppressWarnings("ParameterNumber")
    private Map<String, Model> registerModels(final ResourceSet set,
        final ExecEnv environment,
        final Map<ModelType, List<NamedModel>> config) {
        final Map<String, Model> result = new HashMap<>();
        for (final ModelType type : config.keySet()) {
            for (final NamedModel ref : config.get(type)) {
                final Model model = EmftvmFactory.eINSTANCE.createModel();
                final URI uri = URI.createURI(ref.getPath().getAbsolutePath());
                switch (type) {
                    case INPUT:
                        model.setResource(set.getResource(uri, true));
                        environment.registerInputModel(ref.getName(), model);
                        break;
                    case OUTPUT:
                        model.setResource(set.createResource(uri));
                        environment.registerOutputModel(ref.getName(), model);
                        break;
                    case IN_OUT:
                        model.setResource(set.getResource(uri, true));
                        environment.registerInOutModel(ref.getName(), model);
                        break;
                    default:
                        // Do nothing
                }
                result.put(ref.getName(), model);
            }
        }
        return result;
    }

    /**
     * Loads the metamodels in the resource set and makes them available in the
     * execution environment.
     * @param set The resource set
     * @param environment The execution environment
     */
    private void registerMetamodels(final ResourceSet set,
        final ExecEnv environment) {
        for (final Map.Entry<String, URI> entry : this.metamodels.entrySet()) {
            final Metamodel metamodel =
                EmftvmFactory.eINSTANCE.createMetamodel();
            metamodel.setResource(set.getResource(entry.getValue(), true));
            environment.registerMetaModel(entry.getKey(), metamodel);
        }
    }

    /**
     * Create and register resource factories to read/parse .xmi and .emftvm
     * files. The latter corresponds to files created by the transformation
     * compiler (ATL-EMFTV compiler).
     * @param set The resource set
     */
    private void registerFactories(final ResourceSet set) {
        final XMIResourceFactoryImpl factory = new XMIResourceFactoryImpl();
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("ecore", factory);
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("xmi", factory);
        set.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put("emftvm", new EMFTVMResourceFactoryImpl());
    }

    /**
     * A pair of variable name and file path.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    @Data
    @AllArgsConstructor
    public static final class NamedModel {

        /**
         * The variable name of the model in the ATL module.
         */
        private final String name;

        /**
         * The path to the XMI file.
         */
        private final File path;
    }

    /**
     * ATL model types.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    public enum ModelType {
        /**
         * Input models.
         */
        INPUT,

        /**
         * Output models.
         */
        OUTPUT,

        /**
         * InOut models.
         */
        IN_OUT
    }

    /**
     * An ATL transformation builder.
     * @author Miguel Jimenez (miguel@uvic.ca)
     * @version $Id$
     * @since 0.1.0
     */
    public static final class Builder {

        /**
         * The ecore extension.
         */
        private static final String ECORE_EXT = ".ecore";

        /**
         * The metamodels.
         */
        private final Map<String, URI> metamodels;

        /**
         * The input, output and in-out models.
         */
        private final Map<ModelType, List<NamedModel>> models;

        /**
         * The transformation.
         */
        private File transformation;

        /**
         * Default constructor.
         */
        public Builder() {
            this.metamodels = new HashMap<>();
            this.models = Builder.emptyModels();
        }

        /**
         * Initialize the list of models.
         * @return A map
         */
        private static Map<ModelType, List<NamedModel>> emptyModels() {
            final Map<ModelType, List<NamedModel>> models = new HashMap<>();
            models.put(ModelType.INPUT, new ArrayList<>());
            models.put(ModelType.IN_OUT, new ArrayList<>());
            models.put(ModelType.OUTPUT, new ArrayList<>());
            return models;
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
         * @param name The name of the metamodel
         * @param path The path to the metamodel
         * @return This builder
         */
        public Builder withMetamodel(final String name, final String path) {
            this.metamodels.put(name, this.nsUri(new File(path)));
            return this;
        }

        /**
         * Adds a metamodel to the transformation context.
         * @param epackage The metamodel's EPackage
         * @return This builder
         */
        public Builder withMetamodel(final EPackage epackage) {
            this.metamodels.put(
                epackage.getName(),
                URI.createURI(epackage.getNsURI())
            );
            return this;
        }

        /**
         * Adds a model to the transformation context.
         * @param type The type of the model
         * @param name The name of the ATL variable (e.g., OUT)
         * @param model The model object
         * @return This builder
         * @throws IOException {@link #asFile(EObject)}
         */
        public Builder withModel(final ModelType type, final String name,
            final EObject model) throws IOException {
            return this.withModel(type, name, this.asFile(model).getPath());
        }

        /**
         * Adds a model to the transformation context.
         * @param type The type of the model
         * @param name The name of the ATL variable (e.g., OUT)
         * @param path The path to the output model
         * @return This builder
         */
        public Builder withModel(final ModelType type, final String name,
            final String path) {
            this.models
                .get(type)
                .add(new NamedModel(name, new File(path)));
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
            final File file = File.createTempFile("model", Builder.ECORE_EXT);
            Files.write(
                Paths.get(file.toURI()),
                new SerializationParser().asXml(model).getBytes(),
                StandardOpenOption.TRUNCATE_EXISTING
            );
            return file;
        }

        /**
         * Lazy registration of the metamodel to get the nsUri.
         * @param metamodel The metamodel file to register
         * @return The metamodel's nsUri
         */
        private URI nsUri(final File metamodel) {
            URI uri = URI.createURI("");
            Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .put("ecore", new EcoreResourceFactoryImpl());
            final ResourceSet set = new ResourceSetImpl();
            final ExtendedMetaData metadata = new BasicExtendedMetaData(
                EPackage.Registry.INSTANCE
            );
            set.getLoadOptions()
                .put(XMLResource.OPTION_EXTENDED_META_DATA, metadata);
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
         * Builds the transformation.
         * @return A new ATL transformation
         */
        @SuppressWarnings("PMD.CyclomaticComplexity")
        public AtlTransformation build() {
            if (this.transformation == null) {
                throw new IllegalArgumentException(
                    "The transformation module is required");
            }
            if (this.metamodels.isEmpty()) {
                throw new IllegalArgumentException(
                    "At least one metamodel is required");
            }
            if (this.models.get(ModelType.INPUT).isEmpty()) {
                throw new IllegalArgumentException(
                    "At least one input model is required");
            }
            if (this.models.get(ModelType.OUTPUT).isEmpty()
                && this.models.get(ModelType.IN_OUT).isEmpty()) {
                throw new IllegalArgumentException(
                    "At least one output model is required");
            }
            return new AtlTransformation(
                this.metamodels,
                this.models,
                this.transformation
            );
        }
    }

}
