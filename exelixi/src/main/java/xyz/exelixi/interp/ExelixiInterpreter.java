package xyz.exelixi.interp;

import com.google.auto.service.AutoService;
import se.lth.cs.tycho.phases.LiftScopesPhase;
import xyz.exelixi.backend.ExelixiBackend;

/**
 * @author Endri Bezati
 */

@AutoService(ExelixiBackend.class)
public class ExelixiInterpreter extends ExelixiBackend{

    @Override
    public String getId() { return "interp"; }

    @Override
    public String getDescription() {
        return "Exelixi CAL Interpreter";
    }

    @Override
    protected void registerPhases() {
        addPhase(LoadEntityPhase);
        addPhase(LoadPreludePhase);
        addPhase(LoadImportsPhase);

        // For debugging
        addPhase(PrintLoadedSourceUnits);
        addPhase(PrintTreesPhase);
        addPhase(PrettyPrintPhase);

        // Post parse
        addPhase(RemoveExternStubPhase);
        addPhase(OperatorParsingPhase);

        // Name and type analyses and transformations
        addPhase(DeclarationAnalysisPhase);
        addPhase(ImportAnalysisPhase);
        addPhase(NameAnalysisPhase);
        addPhase(TypeAnnotationAnalysisPhase);
        addPhase(TypeAnalysisPhase);
        addPhase(AddTypeAnnotationsPhase);

        addPhase(CreateNetworkPhase);
        addPhase(ResolveGlobalEntityNamesPhase);
        addPhase(ResolveGlobalVariableNamesPhase);
        addPhase(ElaborateNetworkPhase);
        addPhase(DeadDeclEliminationPhase);
        addPhase(ComputeClosuresPhase);

        // Actor transformations
        addPhase(RenameActorVariablesPhase);
        addPhase(LiftProcessVarDeclsPhase);
        addPhase(ProcessToCalPhase);
        addPhase(AddSchedulePhase);
        addPhase(ScheduleUntaggedPhase);
        addPhase(ScheduleInitializersPhase);
        addPhase(CloneTreePhase);
        addPhase(MergeManyGuardsPhase);
        addPhase(CalToAmPhase);
        addPhase(RemoveEmptyTransitionsPhase);
        addPhase(ReduceActorMachinePhase);
        addPhase(CompositionEntitiesUniquePhase);
        addPhase(CompositionPhase);
        addPhase(InternalizeBuffersPhase);
        addPhase(RemoveUnusedConditionsPhase);
        addPhase(LiftScopesPhase);

        // Code generations
        addPhase(RemoveUnusedEntityDeclsPhase);
        addPhase(PrintNetworkPhase);
        addPhase(new ExelixiInterpreterPhase());
    }

}
