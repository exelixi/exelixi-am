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
package xyz.exelixi.utils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.comp.SourceUnit;
import se.lth.cs.tycho.ir.Port;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.entity.cal.CalActor;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;

import java.util.*;

/**
 * This class contains the network object resolver to help during the code generation process
 *
 * @author Simone Casale-Brunet
 */
public class Resolver {

    private final Network network;
    private final Map<String, Instance> instancesMap;
    private final List<Connection> connectionsList;
    private Table<Pair<String, String>, Pair<String, String>, Connection> connectionsTable;
    private Map<Pair<String, String>, List<Connection>> outputConnestionsMap;
    private Map<Pair<String, String>, Connection> inputConnectionsMap;
    private Map<String, Map<Connection, PortDecl>> incomings;
    private Map<String, Map<Connection, PortDecl>> outgoings;
    private Map<Instance, CalActor> entityDeclMap;
    private List<Connection> inputs;
    private List<Connection> outputs;
    private Map<Connection, Pair<PortDecl, PortDecl>> connectionsPortsMap;


    /**
     * Constructor keep private
     *
     * @param task
     */
    private Resolver(CompilationTask task) {
        this.network = task.getNetwork();

        entityDeclMap = new HashMap<>();
        instancesMap = new HashMap<>();
        incomings = new HashMap<>();
        outgoings = new HashMap<>();
        inputs = new ArrayList<>();
        outputs = new ArrayList<>();
        connectionsPortsMap = new HashMap<>();

        for (Instance instance : network.getInstances()) {
            instancesMap.put(instance.getInstanceName(), instance);

            CalActor actor = (CalActor) task.getSourceUnits().stream()
                    .map(SourceUnit::getTree)
                    .filter(ns -> ns.getQID().equals(instance.getEntityName().getButLast()))
                    .flatMap(ns -> ns.getEntityDecls().stream())
                    .filter(decl -> decl.getName().equals(instance.getEntityName().getLast().toString()))
                    .findFirst().get().getEntity();
            entityDeclMap.put(instance, actor);

            incomings.put(instance.getInstanceName(), new HashMap<>());
            outgoings.put(instance.getInstanceName(), new HashMap<>());
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

            Pair<String, String> source = Pair.of(srcInstance, srcPort);
            Pair<String, String> target = Pair.of(tgtInstance, tgtPort);

            PortDecl sourcePortDecl = null;
            PortDecl targetPortDecl = null;

            // of the global table
            if (srcInstance != null && tgtInstance != null) {
                connectionsTable.put(source, target, connection);
            }

            // outgoing connections of an instance
            if (srcInstance != null) {
                outputConnestionsMap.putIfAbsent(source, new ArrayList<>());
                outputConnestionsMap.get(source).add(connection);

                Instance instance = instancesMap.get(srcInstance);
                CalActor actor = entityDeclMap.get(instance);
                sourcePortDecl = actor.getOutputPorts().stream().filter(p -> p.getName().equals(srcPort)).findFirst().get();

                outgoings.get(srcInstance).put(connection, sourcePortDecl);
            } else {
                inputs.add(connection);
            }

            // incoming connection of an instance
            if (tgtInstance != null) {
                inputConnectionsMap.put(target, connection);

                Instance instance = instancesMap.get(tgtInstance);
                CalActor actor = entityDeclMap.get(instance);
                targetPortDecl = actor.getInputPorts().stream().filter(p -> p.getName().equals(tgtPort)).findFirst().get();
                incomings.get(tgtInstance).put(connection, targetPortDecl);
            } else {
                outputs.add(connection);
            }

            connectionsPortsMap.put(connection, Pair.of(sourcePortDecl, targetPortDecl));
        }

    }

    /**
     * Get the instance with the given name
     *
     * @param name the instance name
     * @return the instance
     */
    public Instance getInstace(String name) {
        if (!instancesMap.containsKey(name)) {
            new NoSuchElementException("No such instance");
        }
        return instancesMap.get(name);
    }

    /**
     * Get the connection unique identifier
     *
     * @param connection the connection
     * @return the connection id
     */
    public int getConnectionId(Connection connection) {
        if (!connectionsList.contains(connection)) {
            new NoSuchElementException("No such connection");
        }
        return connectionsList.indexOf(connection);
    }

    /**
     * Create a new resolver
     *
     * @param task
     * @return
     */
    public static Resolver create(CompilationTask task) {
        return new Resolver(task);
    }

    /**
     * Get the incoming connection of an couple (instance, port). <code>null</code> if not present
     *
     * @param instance  the instance name
     * @param inputPort the port name
     * @return
     */
    public Connection getIncoming(String instance, String inputPort) {
        return inputConnectionsMap.get(Pair.of(instance, inputPort));
    }

    /**
     * Ge the map of incomings (connection, port) of the given instance
     *
     * @param instance
     * @return
     */
    public Map<Connection, PortDecl> getIncomingsMap(String instance) {
        return Collections.unmodifiableMap(incomings.getOrDefault(instance, Collections.emptyMap()));
    }

    /**
     * Ge the map of outgoings (connection, port) of the given instance
     *
     * @param instance
     * @return
     */
    public Map<Connection, PortDecl> getOutgoingsMap(String instance) {
        return Collections.unmodifiableMap(outgoings.getOrDefault(instance, Collections.emptyMap()));
    }


    /**
     * Get the outputPort connection list of an couple (instance, port)
     *
     * @param instance   the instance name
     * @param outputPort the port name
     * @return
     */
    public List<Connection> getOutgoings(String instance, String outputPort) {
        return Collections.unmodifiableList(outputConnestionsMap.getOrDefault(Pair.of(instance, outputPort), Collections.emptyList()));
    }

    /**
     * Get the incoming connections of a network
     *
     * @return
     */
    public List<Connection> getIncomings() {
        return Collections.unmodifiableList(inputs);
    }

    /**
     * Get the output connections of network
     *
     * @return
     */
    public List<Connection> getOutgoings() {
        return Collections.unmodifiableList(outputs);
    }


    /**
     * Get the source port declaration of the given connection.
     * <code>null</code> if it does not exists (i.e. the connection is not valid or the connection has not an attached port)
     *
     * @param connection
     * @return
     */
    public PortDecl getSourcePortDecl(Connection connection) {
        if (connectionsPortsMap.containsKey(connection)) {
            return connectionsPortsMap.get(connection).v1;
        }
        return null;
    }

    /**
     * Get the target port declaration of the given connection.
     * <code>null</code> if it does not exists (i.e. the connection is not valid or the connection has not an attached port)
     *
     * @param connection
     * @return
     */
    public PortDecl getTargetPortDecl(Connection connection) {
        if (connectionsPortsMap.containsKey(connection)) {
            return connectionsPortsMap.get(connection).v2;
        }
        return null;
    }

}
