package xyz.exelixi.utils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;

import java.util.*;

/**
 * Created by scb on 2/3/17.
 */
public class ModelHelper {

    private final Network network;
    private final Map<String, Instance> instancesMap;
    private final List<Connection> connectionsList;
    private Table<Pair<String, String>, Pair<String, String>, Connection> connectionsTable;
    private Map<Pair<String, String>, List<Connection>> outputConnestionsMap;
    private Map<Pair<String, String>, Connection> inputConnectionsMap;
    private Map<String, List<Pair<PortDecl, Connection>>> incomings;
    private Map<String, List<Pair<PortDecl, Connection>>> outgoings;
    private Map<Instance, CalActor> entityDeclMap;
    private List<Connection> inputs;
    private List<Connection> outputs;


    private ModelHelper(CompilationTask task) {
        this.network = task.getNetwork();

        entityDeclMap = new HashMap<>();
        instancesMap = new HashMap<>();
        incomings = new HashMap<>();
        outgoings = new HashMap<>();
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();

        for (Instance instance : network.getInstances()) {
            instancesMap.put(instance.getInstanceName(), instance);

            CalActor actor = (CalActor) task.getSourceUnits().stream()
                    .map(SourceUnit::getTree)
                    .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                    .flatMap(ns -> ns.getEntityDecls().stream())
                    .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                    .findFirst().get().getEntity();
            entityDeclMap.put(instance, actor);

            incomings.put(instance.getInstanceName(), new ArrayList<>());
            outgoings.put(instance.getInstanceName(), new ArrayList<>());
        }

        connectionsList = new ArrayList<>(network.getConnections());
        connectionsTable = HashBasedTable.create();
        outputConnestionsMap = new HashMap<>();
        inputConnectionsMap = new HashMap<>();

        for (Connection connection : connectionsList) {
            String srcPort = connection.getSource().getPort();
            String srcInstance = connection.getSource().getInstance().orElse(null);
            String tgtPort = connection.getTarget().getPort();
            String tgtInstance = connection.getTarget().getInstance().orElse(null);

            Pair<String, String> source = Pair.create(srcInstance, srcPort);
            Pair<String, String> target = Pair.create(tgtInstance, tgtPort);

            // create the global table
            if (srcInstance != null && tgtInstance != null) {
                connectionsTable.put(source, target, connection);
            }

            // outgoing connections of an instance
            if (srcInstance != null) {
                outputConnestionsMap.putIfAbsent(source, new ArrayList<>());
                outputConnestionsMap.get(source).add(connection);

                Instance instance = instancesMap.get(srcInstance);
                CalActor actor = entityDeclMap.get(instance);
                PortDecl port = actor.getOutputPorts().stream().filter(p -> p.getName().equals(srcPort)).findFirst().get();

                outgoings.get(srcInstance).add(Pair.create(port, connection));
            }else{
                inputs.add(connection);
            }

            // incoming connection of an instance
            if (tgtInstance != null) {
                inputConnectionsMap.put(target, connection);

                Instance instance = instancesMap.get(tgtInstance);
                CalActor actor = entityDeclMap.get(instance);
                PortDecl port = actor.getInputPorts().stream().filter(p -> p.getName().equals(tgtPort)).findFirst().get();
                incomings.get(tgtInstance).add(Pair.create(port, connection));
            }else{
                outputs.add(connection);
            }
        }

    }

    public Instance getInstace(String name) {
        return instancesMap.get(name);
    }

    public int getConnectionId(Connection connection) {
        return connectionsList.indexOf(connection);
    }

    public static ModelHelper create(CompilationTask task) {
        return new ModelHelper(task);
    }

    public Connection getIncoming(String instance, String inputPort) { return inputConnectionsMap.get(Pair.create(instance, inputPort));}

    public List<Pair<PortDecl, Connection>> getIncomings(String instance) {return incomings.get(instance);}

    public List<Pair<PortDecl, Connection>> getOutgoings(String instance) { return outgoings.get(instance); }

    public List<Connection> getOutgoings(String instance, String outputPort) { return outputConnestionsMap.getOrDefault(Pair.create(instance, outputPort), Collections.EMPTY_LIST);}

    public List<Connection> getInputs(){
        return Collections.unmodifiableList(inputs);
    }

    public List<Connection> getOutputs(){ return Collections.unmodifiableList(outputs); }

    public List<Connection> getBorders(){
        List<Connection> borders = new ArrayList<>();
        borders.addAll(getInputs());
        borders.addAll(getOutputs());
        return  borders;
    }


}
