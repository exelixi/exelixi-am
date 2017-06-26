package xyz.exelixi.interp;

import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.ir.util.ImmutableList;

/**
 * @author Endri Bezati.
 */
public class ExelixiBasicNetworkSimulator implements Simulator {

    private int defaultStackSize;
    private int defaultChannelSize;
    Network network;
    private Simulator[] simList;
    int nextInstanceToRun;


    public ExelixiBasicNetworkSimulator(CompilationTask task, Network network, int defaultChannelSize, int defaultStackSize) {
        this.network = network;
        this.defaultChannelSize = defaultChannelSize;
        this.defaultStackSize = defaultStackSize;
        ImmutableList<Instance> instanceList = network.getInstances();
        int nbrInstances = instanceList.size();
        simList = new Simulator[nbrInstances];

        Channel[][] internalNodeSinkPortChannel = new Channel[nbrInstances][];            //[nbrInstances][portIndex]
        Channel[][] internalNodeSourcePortChannel = new Channel[nbrInstances][];

        int i = 0;
        for (Instance instance : instanceList) {
            GlobalEntityDecl entityDecl = task.getSourceUnits().stream()
                    .map(SourceUnit::getTree)
                    .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                    .flatMap(ns -> ns.getEntityDecls().stream())
                    .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                    .findFirst().get();

            internalNodeSourcePortChannel[i] = new Channel[entityDecl.getEntity().getOutputPorts().size()];
            internalNodeSinkPortChannel[i] = new Channel[entityDecl.getEntity().getInputPorts().size()];
            i++;
        }

    }


    @Override
    public boolean step() {
        return false;
    }

    @Override
    public void scopesToString(StringBuffer sb) {

    }
}
