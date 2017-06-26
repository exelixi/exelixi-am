package xyz.exelixi.interp;

import se.lth.cs.tycho.ir.IRNode;
import se.lth.cs.tycho.ir.Variable;

public class VariableLocation {

    private int offset;

    private Variable variable;

    public static VariableLocation scopeVariable(String name, int scopeId, int offset) {
        return new VariableLocation(name, scopeId, offset, true);
    }

    public static VariableLocation stackVariable(String name, int offset) {
        return new VariableLocation(name, -1, offset, false);
    }

    private VariableLocation(String name, int scope, int offset, boolean isStatic) {

        variable = Variable.variable(name);
        this.offset = offset;
    }

    VariableLocation copy(String name, int scope, int offset, boolean isStatic) {
        return new VariableLocation(name, scope, offset, isStatic);
    }

    public int getOffset() {
        return offset;
    }

    public String toString() {
        //if (isScopeVariable()) {
        //		return "ScopeVariable(" + getName() + ", " + getScopeId() + ", " + offset + ")";
        //	} else {
        return "Variable(" + variable.getName() + ", " + offset + ")";
        //	}
    }
}
