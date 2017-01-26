package xyz.exelixi.backend;

import se.lth.cs.tycho.phases.*;
import se.lth.cs.tycho.settings.SettingsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Simone Casale-Brunet
 */
public abstract class ExelixiBackend implements Phase {

    // Parse
    public static final Phase LoadEntityPhase = new LoadEntityPhase();

    public static final Phase LoadPreludePhase = new LoadPreludePhase();
    public static final Phase LoadImportsPhase = new LoadImportsPhase();

    // For debugging
    public static final Phase PrintLoadedSourceUnits = new PrintLoadedSourceUnits();
    public static final Phase PrintTreesPhase = new PrintTreesPhase();
    public static final Phase PrettyPrintPhase = new PrettyPrintPhase();

    // Post parse
    public static final Phase RemoveExternStubPhase = new RemoveExternStubPhase();
    public static final Phase OperatorParsingPhase = new OperatorParsingPhase();

    // Name and type analyses and transformations
    public static final Phase DeclarationAnalysisPhase = new DeclarationAnalysisPhase();
    public static final Phase ImportAnalysisPhase = new ImportAnalysisPhase();
    public static final Phase NameAnalysisPhase = new NameAnalysisPhase();
    public static final Phase TypeAnnotationAnalysisPhase = new TypeAnnotationAnalysisPhase();
    public static final Phase TypeAnalysisPhase = new TypeAnalysisPhase();
    public static final Phase AddTypeAnnotationsPhase = new AddTypeAnnotationsPhase();

    public static final Phase CreateNetworkPhase = new CreateNetworkPhase();
    public static final Phase ResolveGlobalEntityNamesPhase = new ResolveGlobalEntityNamesPhase();
    public static final Phase ResolveGlobalVariableNamesPhase = new ResolveGlobalVariableNamesPhase();
    public static final Phase ElaborateNetworkPhase = new ElaborateNetworkPhase();
    public static final Phase DeadDeclEliminationPhase = new DeadDeclEliminationPhase();
    public static final Phase ComputeClosuresPhase = new ComputeClosuresPhase();

    // Actor transformations
    public static final Phase RenameActorVariablesPhase = new RenameActorVariablesPhase();
    public static final Phase LiftProcessVarDeclsPhase = new LiftProcessVarDeclsPhase();
    public static final Phase ProcessToCalPhase = new ProcessToCalPhase();
    public static final Phase AddSchedulePhase = new AddSchedulePhase();
    public static final Phase ScheduleUntaggedPhase = new ScheduleUntaggedPhase();
    public static final Phase ScheduleInitializersPhase = new ScheduleInitializersPhase();
    public static final Phase CloneTreePhase = new CloneTreePhase();
    public static final Phase MergeManyGuardsPhase = new MergeManyGuardsPhase();
    public static final Phase CalToAmPhase = new CalToAmPhase();
    public static final Phase RemoveEmptyTransitionsPhase = new RemoveEmptyTransitionsPhase();
    public static final Phase ReduceActorMachinePhase = new ReduceActorMachinePhase();
    public static final Phase CompositionEntitiesUniquePhase = new CompositionEntitiesUniquePhase();
    public static final Phase CompositionPhase = new CompositionPhase();
    public static final Phase InternalizeBuffersPhase = new InternalizeBuffersPhase();
    public static final Phase RemoveUnusedConditionsPhase = new RemoveUnusedConditionsPhase();

    // Code generations
    public static final Phase RemoveUnusedEntityDeclsPhase = new RemoveUnusedEntityDeclsPhase();
    public static final Phase PrintNetworkPhase = new PrintNetworkPhase();

    private List<Phase> phases;

    public ExelixiBackend() {
        phases = new ArrayList<>();

        registerPhases();

        phases = Collections.unmodifiableList(phases);
    }

    protected abstract void registerPhases();

    protected final void addPhase(Phase phase) {
        if (!phases.contains(phase)) {
            phases.add(phase);
        } else {
            throw new RuntimeException("Phase " + phase.getName() + " already registered in " + getName());
        }
    }

    public List<Phase> getPhases() {
        return phases;
    }


    public abstract String getId();
}
