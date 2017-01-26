package xyz.exelixi.backend.hls;

import com.google.auto.service.AutoService;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.CompilationException;
import se.lth.cs.tycho.settings.Setting;
import xyz.exelixi.backend.ExelixiBackend;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Simone Casale-Brunet
 */
@AutoService(ExelixiBackend.class)
public class HlsBackend extends ExelixiBackend {

    @Override
    public String getId() { return "hls"; }

    @Override
    public String getDescription() {
        return "Vivado HLS Backend Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask compilationTask, Context context) throws CompilationException {
        return null;
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

   // @Override
   // public List<Setting<?>> getPhaseSettings() { return Collections.EMPTY_LIST; }

}
