package xyz.exelixi.backend;

import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.phases.cbackend.Backend;

import java.util.*;

/**
 * Created by scb on 1/26/17.
 */
public class BackendsFactory {

    public static final BackendsFactory INSTANCE = new BackendsFactory();


    private final List<ExelixiBackend> backends;
    private final Collection<Phase> registeredPhases;

    private BackendsFactory() {
        // load the registered backends
        backends = new ArrayList<>();
        ServiceLoader<ExelixiBackend> loader = ServiceLoader.load(ExelixiBackend.class);
        loader.forEach(b -> backends.add(b));

        // find all the registered phases
        registeredPhases = new HashSet<>();
        backends.forEach(b -> registeredPhases.addAll(b.getPhases()));
    }

    public List<ExelixiBackend> getBackends() {
        return backends;
    }

    public Collection<Phase> getRegisteredPhases() {
        return registeredPhases;
    }

}
