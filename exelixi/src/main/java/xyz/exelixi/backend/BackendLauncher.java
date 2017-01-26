package xyz.exelixi.backend;

import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.comp.Loader;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.phases.*;
import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Configuration;
import se.lth.cs.tycho.settings.SettingsManager;
import xyz.exelixi.Settings;
import xyz.exelixi.utils.FrontendLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The interface to a launcher Exelixi compiler
 *
 * @author Simone Casale-Brunet
 */
public abstract class BackendLauncher {


    private final static List<Phase> defaultPhases;

    static {
        defaultPhases = new ArrayList<>(Arrays.asList(
                // Hack: pause to hook up profiler.
//			    new WaitForInputPhase(),

                // Parse
                new LoadEntityPhase(),
                new LoadPreludePhase(),
                new LoadImportsPhase(),

                // For debugging
                new PrintLoadedSourceUnits(),
                new PrintTreesPhase(),
                new PrettyPrintPhase(),

                // Post parse
                new RemoveExternStubPhase(),
                new OperatorParsingPhase(),

                // Name and type analyses and transformations
                new DeclarationAnalysisPhase(),
                new ImportAnalysisPhase(),
                new NameAnalysisPhase(),
                new TypeAnnotationAnalysisPhase(),
                new TypeAnalysisPhase(),
                new AddTypeAnnotationsPhase(),

                new CreateNetworkPhase(),
                new ResolveGlobalEntityNamesPhase(),
                new ResolveGlobalVariableNamesPhase(),
                new ElaborateNetworkPhase(),
                new DeadDeclEliminationPhase(),
                new ComputeClosuresPhase(),

                // Actor transformations
                new RenameActorVariablesPhase(),
                new LiftProcessVarDeclsPhase(),
                new ProcessToCalPhase(),
                new AddSchedulePhase(),
                new ScheduleUntaggedPhase(),
                new ScheduleInitializersPhase(),
                new CloneTreePhase(),
                new MergeManyGuardsPhase(),
                new CalToAmPhase(),
                new RemoveEmptyTransitionsPhase(),
                new ReduceActorMachinePhase(),
                new CompositionEntitiesUniquePhase(),
                new CompositionPhase(),
                new InternalizeBuffersPhase(),
                new RemoveUnusedConditionsPhase(),
                new RemoveUnusedEntityDeclsPhase(),
                new PrintNetworkPhase()
        ));
    }

    protected final List<Phase> phases;
    private Configuration configuration;
    private Context compilationContext;

    //FIXME use an empty constructor
    public BackendLauncher() {
        phases = new ArrayList<>(defaultPhases);
        phases.addAll(getAdditionalPhases());
    }

    public static final SettingsManager getDefaultSettings() {
        return SettingsManager.builder()
                .add(Settings.sourcePaths)
                .add(Settings.orccSourcePaths)
                .add(Settings.xdfSourcePaths)
                .add(Settings.targetPath)
                .add(Reporter.reportingLevel)
                .add(Settings.phaseTimer)
                .add(Loader.followLinks) // TODO or FronteLoader?
                .addAll(defaultPhases.stream()
                        .flatMap(phase -> phase.getPhaseSettings().stream())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * The additional phases implemented by the compiler
     *
     * @return
     */
    protected List<Phase> getAdditionalPhases() {
        return Collections.EMPTY_LIST;
    }

    protected final Configuration getConfiguration() {
        return configuration;
    }

    protected final Context getCompilationContext(){
        return compilationContext;
    }

    public final void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        Reporter reporter = Reporter.instance(configuration);
        Loader loader = FrontendLoader.instance(configuration, reporter);
        this.compilationContext = new Context(configuration, loader, reporter);
        // assert dependenciesSatisfied() : "Unsatisfied phase dependencies.";
    }

    public final List<Phase> getPhases() {
        return phases;
    }

    public abstract boolean compile(QID entity);

    public abstract String getName();

    public final SettingsManager getSpecificSettings() {
        return SettingsManager.builder()
                .addAll(getAdditionalPhases().stream()
                        .flatMap(phase -> phase.getPhaseSettings().stream())
                        .collect(Collectors.toList()))
                .build();
    }

}
