package xyz.exelixi.backend.c.codegen;

import org.multij.Binding;
import org.multij.Module;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.phases.attributes.Types;
import se.lth.cs.tycho.types.CallableType;
import se.lth.cs.tycho.types.Type;
import xyz.exelixi.backend.c.CBackendCore;

import java.util.List;

@Module
public interface Global {
    @Binding
    CBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default Code code() {
        return backend().code();
    }

    default Types types() {
        return backend().types();
    }

    default DefaultValues defVal() {
        return backend().defaultValues();
    }


    default void globalVariables(List<VarDecl> varDecls) {
        declareGlobalVariables(varDecls);
        emitter().emit("");
        initGlobalVariables(varDecls);
    }

    default void declareGlobalVariables(List<VarDecl> varDecls) {
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            String d = code().declaration(type, backend().variables().declarationName(decl));
            String v = defVal().defaultValue(type);
            emitter().emit("%s = %s;", d, v);
        }
    }

    default void declareExternGlobalVariables(List<VarDecl> varDecls) {
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            String d = code().declaration(type, backend().variables().declarationName(decl));
            emitter().emit("extern %s;", d);
        }
    }


    default void initGlobalVariables(List<VarDecl> varDecls) {
        emitter().emit("void init_global_variables() {");
        emitter().increaseIndentation();
        for (VarDecl decl : varDecls) {
            Type type = types().declaredType(decl);
            if (decl.isExternal() && type instanceof CallableType) {
                String wrapperName = backend().callables().externalWrapperFunctionName(decl);
                String variableName = backend().variables().declarationName(decl);
                String t = backend().callables().mangle(type).encode();
                emitter().emit("%s = (%s) { *%s, NULL };", variableName, t, wrapperName);
            } else {
                code().assign(type, backend().variables().declarationName(decl), decl.getValue());
            }
        }
        emitter().decreaseIndentation();
        emitter().emit("}");
        emitter().emit("");
    }

}
