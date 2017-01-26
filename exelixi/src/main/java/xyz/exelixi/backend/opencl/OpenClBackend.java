package xyz.exelixi.backend.opencl;

import com.google.auto.service.AutoService;
import xyz.exelixi.backend.ExelixiBackend;

/**
 * @author Simone Casale-Brunet
 */
@AutoService(ExelixiBackend.class)
public class OpenClBackend extends ExelixiBackend {

    @Override
    public String getId() {
        return "opencl";
    }

    @Override
    public String getDescription() {
        return "Cpp Backend";
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

        // Code generations
        addPhase(RemoveUnusedEntityDeclsPhase);
        addPhase(PrintNetworkPhase);
    }

}
