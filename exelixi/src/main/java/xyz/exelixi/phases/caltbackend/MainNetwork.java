package xyz.exelixi.phases.caltbackend;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.comp.CompilationTask;
import se.lth.cs.tycho.ir.Parameter;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.network.Connection;
import se.lth.cs.tycho.ir.network.Instance;
import se.lth.cs.tycho.ir.network.Network;
import se.lth.cs.tycho.phases.attributes.GlobalNames;
import se.lth.cs.tycho.phases.attributes.Names;
import se.lth.cs.tycho.types.Type;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Module
public interface MainNetwork {
    @Binding
    ExelixiBackend backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Names names() {
        return backend().names();
    }

    default GlobalNames globalNames() {
        return backend().globalNames();
    }

    default Code code() {
        return backend().code();
    }

    default Preprocessor preprocessor() {
        return backend().preprocessor();
    }

    default void generateCode() {
        CompilationTask task = backend().task();
        Network network = task.getNetwork();
        List<VarDecl> varDecls = task.getSourceUnits().stream().flatMap(unit -> unit.getTree().getVarDecls().stream()).collect(Collectors.toList());

        includeHeaders();

        backend().global().globalVariables(varDecls);

        volatileInterrupted();

        declareRun(network);
    }

    default void volatileInterrupted() {
        emitter().emit("extern _Bool interrupted;");
        emitter().emit("");
    }

    default void includeHeaders() {
        preprocessor().preprocessor_system_include("stdio");
        preprocessor().preprocessor_system_include("stdlib");
        preprocessor().preprocessor_system_include("stdbool");
        preprocessor().preprocessor_system_include("inttypes");
        preprocessor().preprocessor_system_include("string");
        preprocessor().preprocessor_user_include("fifo");
        preprocessor().preprocessor_user_include("callables");
        CompilationTask task = backend().task();
        List<GlobalEntityDecl> entityDecls = task.getSourceUnits().stream().flatMap(unit -> unit.getTree().getEntityDecls().stream()).collect(Collectors.toList());
        for (GlobalEntityDecl globalEntityDecl : entityDecls) {
            String entityName = globalEntityDecl.getName();
            preprocessor().preprocessor_user_include(entityName);
        }
        emitter().emit("");
    }

    default void declareRun(Network network) {
        List<Connection> connections = network.getConnections();
        List<Instance> instances = network.getInstances();


        emitter().emit("void run(int argc, char **argv) {");
        emitter().increaseIndentation();

        emitter().emit("init_global_variables();");

        int nbrOfPorts = network.getInputPorts().size() + network.getOutputPorts().size();
        emitter().emit("if (argc != %d) {", nbrOfPorts + 1);
        emitter().increaseIndentation();
        emitter().emit("fprintf(stderr, \"Wrong number of arguments. Expected %d but was %%d\\n\", argc-1);", nbrOfPorts);
        String args = Stream.concat(network.getInputPorts().stream(), network.getOutputPorts().stream())
                .map(PortDecl::getName)
                .collect(Collectors.joining("> <", "<", ">"));
        emitter().emit("fprintf(stderr, \"Usage: %%s %s\\n\", argv[0]);", args);
        emitter().emit("return;");
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");

        Map<Connection.End, String> connectionNames = new HashMap<>();
        Map<Connection.End, String> connectionTypes = new HashMap<>();
        Map<Connection.End, PortDecl> targetPorts = new LinkedHashMap<>();
        Map<Connection.End, List<Connection.End>> srcToTgt = new HashMap<>();

        for (PortDecl outputPort : network.getOutputPorts()) {
            targetPorts.put(new Connection.End(Optional.empty(), outputPort.getName()), outputPort);
        }
        for (Instance inst : instances) {
            GlobalEntityDecl entityDecl = globalNames().entityDecl(inst.getEntityName(), true);
            Optional<String> instanceName = Optional.of(inst.getInstanceName());
            for (PortDecl inputPort : entityDecl.getEntity().getInputPorts()) {
                Connection.End tgt = new Connection.End(instanceName, inputPort.getName());
                targetPorts.put(tgt, inputPort);
            }
        }
        for (Connection connection : connections) {
            Connection.End src = connection.getSource();
            Connection.End tgt = connection.getTarget();
            srcToTgt.computeIfAbsent(src, x -> new ArrayList<>())
                    .add(tgt);
        }

        {
            int i = 0;
            for (Map.Entry<Connection.End, PortDecl> targetPort : targetPorts.entrySet()) {
                Type tokenType = backend().types().declaredPortType(targetPort.getValue());
                String typeName = code().type(tokenType);
                String channelName = "channel_" + i;
                connectionTypes.put(targetPort.getKey(), typeName);
                connectionNames.put(targetPort.getKey(), channelName);
                emitter().emit("channel_%s %s;", typeName, channelName);
                emitter().emit("channel_create_%s(&%s);", typeName, channelName);
                i = i + 1;
            }
        }
        emitter().emit("");
        for (Instance instance : instances) {
            emitter().emit("static %s_state %s;", instance.getEntityName().getLast(), instance.getInstanceName());
            emitter().emit("memset(&%s, 0, sizeof(%s_state));", instance.getInstanceName(), instance.getEntityName().getLast());
        }
        for (Instance instance : instances) {
            List<String> initParameters = new ArrayList<>();
            initParameters.add("&" + instance.getInstanceName());
            GlobalEntityDecl entityDecl = globalNames().entityDecl(instance.getEntityName(), true);
            for (VarDecl par : entityDecl.getEntity().getValueParameters()) {
                boolean assigned = false;
                for (Parameter<Expression, ?> assignment : instance.getValueParameters()) {
                    if (par.getName().equals(assignment.getName())) {
                        initParameters.add(code().evaluate(assignment.getValue()));
                        assigned = true;
                    }
                }
                if (!assigned) {
                    throw new RuntimeException(String.format("Could not assign to %s. Candidates: {%s}.", par.getName(), String.join(", ", instance.getValueParameters().map(Parameter::getName))));
                }
            }

            for (PortDecl port : entityDecl.getEntity().getInputPorts()) {
                Connection.End end = new Connection.End(Optional.of(instance.getInstanceName()), port.getName());
                initParameters.add("&" + connectionNames.get(end));
            }
            for (PortDecl port : entityDecl.getEntity().getOutputPorts()) {
                Connection.End end = new Connection.End(Optional.of(instance.getInstanceName()), port.getName());
                List<Connection.End> outgoing = srcToTgt.getOrDefault(end, Collections.emptyList());
                String channels = outgoing.stream().map(connectionNames::get).map(c -> "&" + c).collect(Collectors.joining(", "));
                String tokenType = code().type(backend().types().declaredPortType(port));
                emitter().emit("channel_%s *%s_%s[%d] = { %s };", tokenType, instance.getInstanceName(), port.getName(), outgoing.size(), channels);
                initParameters.add(String.format("%s_%s", instance.getInstanceName(), port.getName()));
                initParameters.add(Integer.toString(outgoing.size()));
            }
            emitter().emit("%s_init_actor(%s);", instance.getEntityName().getLast(), String.join(", ", initParameters));
            emitter().emit("");
        }

        int argi = 1;
        for (PortDecl port : network.getInputPorts()) {
            Connection.End end = new Connection.End(Optional.empty(), port.getName());
            List<Connection.End> outgoing = srcToTgt.getOrDefault(end, Collections.emptyList());
            String channels = outgoing.stream().map(connectionNames::get).collect(Collectors.joining(", "));
            emitter().emit("FILE *%s_input_file = fopen(argv[%d], \"r\");", port.getName(), argi);
            String tokenType = code().type(backend().types().declaredPortType(port));
            emitter().emit("channel_%s *%s_channels[%d] = { &%s };", tokenType, port.getName(), outgoing.size(), channels);
            String type = backend().code().type(backend().types().declaredPortType(port));
            emitter().emit("input_actor_%s *%s_input_actor = input_actor_create_%1$s(%2$s_input_file, %2$s_channels, %d);", type, port.getName(), outgoing.size());
            emitter().emit("");
            argi = argi + 1;
        }
        for (PortDecl port : network.getOutputPorts()) {
            Connection.End end = new Connection.End(Optional.empty(), port.getName());
            String channel = connectionNames.get(end);
            emitter().emit("FILE *%s_output_file = fopen(argv[%d], \"w\");", port.getName(), argi);
            String type = backend().code().type(backend().types().declaredPortType(port));
            emitter().emit("output_actor_%s *%s_output_actor = output_actor_create_%1$s(%2$s_output_file, &%s);", type, port.getName(), channel);
            emitter().emit("");
            argi = argi + 1;
        }

        emitter().emit("_Bool progress;");
        emitter().emit("do {");
        emitter().increaseIndentation();
        emitter().emit("progress = false;");
        for (PortDecl inputPort : network.getInputPorts()) {
            emitter().emit("progress |= input_actor_run_%s(%s_input_actor);", code().type(backend().types().declaredPortType(inputPort)), inputPort.getName());
        }
        for (Instance instance : instances) {
            emitter().emit("progress |= %s_run(&%s);", instance.getEntityName().getLast(), instance.getInstanceName());
        }
        for (PortDecl outputPort : network.getOutputPorts()) {
            emitter().emit("progress |= output_actor_run_%s(%s_output_actor);", code().type(backend().types().declaredPortType(outputPort)), outputPort.getName());
        }
        emitter().decreaseIndentation();
        emitter().emit("} while (progress && !interrupted);");
        emitter().emit("");


        for (Map.Entry<Connection.End, String> nameEntry : connectionNames.entrySet()) {
            String name = nameEntry.getValue();
            String type = connectionTypes.get(nameEntry.getKey());
            emitter().emit("channel_destroy_%s(&%s);", type, name);
        }


        for (PortDecl port : network.getInputPorts()) {
            emitter().emit("fclose(%s_input_file);", port.getName());
        }

        for (PortDecl port : network.getOutputPorts()) {
            emitter().emit("fclose(%s_output_file);", port.getName());
        }

        for (PortDecl port : network.getInputPorts()) {
            emitter().emit("input_actor_destroy_%s(%s_input_actor);", code().type(backend().types().declaredPortType(port)), port.getName());
        }

        for (PortDecl port : network.getOutputPorts()) {
            emitter().emit("output_actor_destroy_%s(%s_output_actor);", code().type(backend().types().declaredPortType(port)), port.getName());
        }

        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
        emitter().emit("");
    }


}
