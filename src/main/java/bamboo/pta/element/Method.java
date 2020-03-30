package bamboo.pta.element;

import bamboo.pta.statement.Statement;

import java.util.List;
import java.util.Set;

public interface Method {

    boolean isInstance();

    boolean isStatic();

    boolean isNative();

    Type getClassType();

    String getName();

    Variable getThis();

    List<Variable> getParameters();

    Set<Variable> getReturnVariables();

    Set<Statement> getStatements();
}