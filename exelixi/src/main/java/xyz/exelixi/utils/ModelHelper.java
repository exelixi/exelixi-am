package xyz.exelixi.utils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import se.lth.cs.tycho.comp.CompilationTask;
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


    private ModelHelper(Network network) {
        this.network = network;

        instancesMap = new HashMap<>();
        for (Instance instance : network.getInstances()) {
            instancesMap.put(instance.getInstanceName(), instance);
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
            }

            // incoming connection of an instance
            if (tgtInstance != null) {
                inputConnectionsMap.put(target, connection);
            }
        }

    }

    public Instance getInstace(String name) {
        return instancesMap.get(name);
    }

    public int getConnectionId(Connection connection) {
        return connectionsList.indexOf(connection);
    }

    public static ModelHelper create(Network network) {
        return new ModelHelper(network);
    }

    public Connection getIncoming(String instance, String inputPort) {
        return inputConnectionsMap.get(Pair.create(instance, inputPort));
    }

    public List<Connection> getOutgoings(String instance, String outputPort) {
        return outputConnestionsMap.getOrDefault(Pair.create(instance, outputPort), Collections.EMPTY_LIST);
    }


}
