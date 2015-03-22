package neige.lang;

import neige.lang.expr.Expression;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.MathContext;
import java.util.Map;
import java.util.Set;

public interface InterpContext {
    Expression get(String name);
    void put(String name, Expression exp);
    boolean containsKey(String name);
    Set<Map.Entry<String,Expression>> entrySet();
    MathContext getMathContext();

    boolean isDebugging();
    boolean isAlive();
    void kill();

    InputStream in();
    PrintStream out();
    PrintStream err();

    InterpContext newChild();
}
