package xyz.exelixi.backend.opencl.aocl.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.decl.GlobalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.phases.cbackend.Backend;
import se.lth.cs.tycho.phases.cbackend.Emitter;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.opencl.aocl.AoclBackend;
import xyz.exelixi.backend.opencl.aocl.AoclBackendCore;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by scb on 2/1/17.
 */
@Module
public interface Global {

    @Binding
    AoclBackendCore backend();

    default Types types() {
        return backend().types();
    }

    //default Code code() { return backend().code(); }
    default Code code() {
        return backend().code();
    }

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateGlobalHeader( Path path){
        emitter().open(path.resolve(path.resolve("global.h")));
        emitter().emit("#ifndef GLOBAL_H");
        emitter().emit("#define GLOBAL_H");
        emitter().emit("");
        globalVariableDeclarations(getGlobalVarDecls());
        emitter().emit("");
        emitter().emit("#endif");
        emitter().close();
    }

    default void globalVariableDeclarations(List<GlobalVarDecl> varDecls){
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            if (decl.isExternal() && type instanceof CallableType) {
                throw new Error("Not supported");
                /*
                String wrapperName = backend().callables().externalWrapperFunctionName(decl);
                String variableName = backend().variables().declarationName(decl);
                String t = backend().callables().mangle(type).encode();
                emitter().emit("%s = (%s) { *%s, NULL };", variableName, t, wrapperName);
                */
            } else {
                emitter().emit("#define %s %s", backend().variables().declarationName(decl), code().evaluate(decl.getValue()));
            }
        }
    }


    default List<GlobalVarDecl> getGlobalVarDecls() {
        return backend().task()
                .getSourceUnits().stream()
                .flatMap(unit -> unit.getTree().getVarDecls().stream())
                .collect(Collectors.toList());
    }


}
