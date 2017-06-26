package xyz.exelixi.interp;

import org.multij.MultiJ;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.Context;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.reporting.CompilationException;

import java.io.IOException;

/**
 * @author Endri Bezati
 */
public class ExelixiInterpreterPhase implements Phase {
    @Override
    public String getDescription() {
        return "Exelixi Interpreter Phase";
    }

    @Override
    public CompilationTask execute(CompilationTask task, Context context) throws CompilationException {
        System.out.println("Exelixi Interpreter");

        ExelixiInterpreterCore core = openBackend(task, context);

        // -- Channels
        Network network = task.getNetwork();
        int defaultChannelSize = 512;
        int defaultStackSize = 100;

        ExelixiBasicNetworkSimulator networkSimulator = new ExelixiBasicNetworkSimulator(task, network, defaultChannelSize, defaultStackSize);


        return null;
    }

    /**
     * Get the Backend
     *
     * @param task
     * @param context
     * @return
     * @throws IOException
     */
    private ExelixiInterpreterCore openBackend(CompilationTask task, Context context) {
        return MultiJ.from(ExelixiInterpreterCore.class)
                .bind("task").to(task)
                .bind("context").to(context)
                .instance();
    }

}
