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

import se.lth.cs.tycho.phases.Phase;

import java.util.*;

/**
 * Created by scb on 1/26/17.
 */
public class BackendsRegister {

    public static final BackendsRegister INSTANCE = new BackendsRegister();


    private final Map<String, ExelixiBackend> backendsMap;
    private final Collection<Phase> registeredPhases;


    private BackendsRegister() {
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
