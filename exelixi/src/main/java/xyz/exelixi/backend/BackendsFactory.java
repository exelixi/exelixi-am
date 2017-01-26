package xyz.exelixi.backend;

import se.lth.cs.tycho.phases.Phase;
import se.lth.cs.tycho.phases.cbackend.Backend;

import java.util.*;

/**
 * Created by scb on 1/26/17.
 */
public class BackendsFactory {

    public static final BackendsFactory INSTANCE = new BackendsFactory();


    private final Map<String, ExelixiBackend> backendsMap;
    private final Collection<Phase> registeredPhases;


    private BackendsFactory() {
        // load the registered backends
        backendsMap = new HashMap<>();
        ServiceLoader<ExelixiBackend> loader = ServiceLoader.load(ExelixiBackend.class);
        loader.forEach(b -> backendsMap.put(b.getId(), b));

        // find all the registered phases
        registeredPhases = new HashSet<>();
        backendsMap.values().forEach(b -> registeredPhases.addAll(b.getPhases()));

    }

    public Collection<ExelixiBackend> getBackends() {
        return Collections.unmodifiableCollection(backendsMap.values());
    }

    public Collection<Phase> getRegisteredPhases() {
        return Collections.unmodifiableCollection(registeredPhases);
    }

    public ExelixiBackend getBackend(String id) {
        return backendsMap.get(id);
    }

    public boolean hasBackend(String id){return  backendsMap.containsKey(id);}

}
