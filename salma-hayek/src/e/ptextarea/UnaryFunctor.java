package e.ptextarea;

/**
 * Modifies an object.
 */
public interface UnaryFunctor<ReturnType, ArgumentType> {
    public ReturnType evaluate(ArgumentType arg);
}
