package xyz.exelixi.backend.cpp;

import com.google.auto.service.AutoService;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.reporting.CompilationException;
import xyz.exelixi.backend.ExelixiBackend;

/**
 * @author Simone Casale-Brunet
 */
@AutoService(ExelixiBackend.class)
public class CppBackend extends ExelixiBackend {

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

    public String getId() {
        return "cpp";
    }

    @Override
    public String getDescription() {
        return "Cpp Backend Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask compilationTask, Context context) throws CompilationException {
        return null;
    }
}
