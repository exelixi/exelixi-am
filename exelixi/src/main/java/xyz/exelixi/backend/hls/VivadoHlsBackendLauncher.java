package xyz.exelixi.backend.hls;

import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.phases.PrintNetworkPhase;
import se.lth.cs.tycho.phases.RemoveUnusedEntityDeclsPhase;
import se.lth.cs.tycho.reporting.Diagnostic;
import xyz.exelixi.Settings;
import xyz.exelixi.backend.BackendLauncher;
import xyz.exelixi.phases.VivadoHLSBackendPhase;
import xyz.exelixi.utils.FrontendLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by scb on 1/24/17.
 */
public class VivadoHlsBackendLauncher extends BackendLauncher{


    @Override
    protected List<Phase> getAdditionalPhases() {
        return Arrays.asList(// Code generations

                new VivadoHLSBackendPhase());
    }

    /*
    protected Class<FrontendLoader> getLoader(){
        return FrontendLoader.class;
    }*/

    @Override
    public boolean compile(QID entity) {
        Context compilationContext = getCompilationContext();
        CompilationTask compilationTask = new CompilationTask(Collections.emptyList(), entity, null);
        long[] phaseExecutionTime = new long[phases.size()];
        int currentPhaseNumber = 0;
        boolean success = true;
        for (Phase phase : phases) {
            long startTime = System.nanoTime();
            compilationTask = phase.execute(compilationTask, compilationContext);
            phaseExecutionTime[currentPhaseNumber] = System.nanoTime() - startTime;
            currentPhaseNumber += 1;
            if (compilationContext.getReporter().getMessageCount(Diagnostic.Kind.ERROR) > 0) {
                success = false;
                break;
            }
        }
        if (compilationContext.getConfiguration().get(Settings.phaseTimer)) {
            System.out.println("Execution time report:");
            for (int j = 0; j < currentPhaseNumber; j++) {
                System.out.println(phases.get(j).getName() + " (" + phaseExecutionTime[j] / 1_000_000 + " ms)");
            }
        }
        return success;
    }

    @Override
    public String getName() {
        return "vivado-hls";
    }
}
