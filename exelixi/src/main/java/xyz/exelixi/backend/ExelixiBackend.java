/*
 * EXELIXI
 *
 * Copyright (C) 2017 EPFL SCI-STI-MM
 *
 * This file is part of EXELIXI.
 *
 * EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the covered work.
 *
 */
package xyz.exelixi.backend;

import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.comp.Loader;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.phases.*;
import se.lth.cs.tycho.reporting.CompilationException;
import se.lth.cs.tycho.reporting.Diagnostic;
import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Configuration;
import xyz.exelixi.frontend.FrontendLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static xyz.exelixi.Settings.phaseTimer;

/**
 * @author Simone Casale-Brunet
 */
public abstract class ExelixiBackend {

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
    public static final Phase LiftScopesPhase = new LiftScopesPhase();

    // Code generations
    public static final Phase RemoveUnusedEntityDeclsPhase = new RemoveUnusedEntityDeclsPhase();
    public static final Phase PrintNetworkPhase = new PrintNetworkPhase();

    private List<Phase> phases;
    protected Configuration configuration;
    protected Context compilationContext;

    public ExelixiBackend() {
        phases = new ArrayList<>();
        registerPhases();
    }

    protected abstract void registerPhases();

    protected final void addPhase(Phase phase) {
        if (!phases.contains(phase)) {
            phases.add(phase);
        } else {
            throw new RuntimeException("Phase " + phase.getName() + " already registered in " + getId());
        }
    }

    public List<Phase> getPhases() {
        return Collections.unmodifiableList(phases);
    }

    public void setConfiguration(Configuration configuration){
        this.configuration = configuration;
        Reporter reporter = Reporter.instance(configuration);
        Loader loader = FrontendLoader.instance(configuration, reporter);
        this.compilationContext = new Context(configuration, loader, reporter);
    }

    public boolean compile(QID entity) {
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
        if (compilationContext.getConfiguration().get(phaseTimer)) {
            System.out.println("Execution time report:");
            for (int j = 0; j < currentPhaseNumber; j++) {
                System.out.println(phases.get(j).getName() + " (" + phaseExecutionTime[j] / 1_000_000 + " ms)");
            }
        }

        return success;
    }


    public abstract String getId();

    public abstract String getDescription();


}
