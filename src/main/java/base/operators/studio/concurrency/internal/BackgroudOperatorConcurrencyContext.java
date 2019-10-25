package base.operators.studio.concurrency.internal;

import base.operators.RapidMiner;
import base.operators.operator.Operator;

public class BackgroudOperatorConcurrencyContext extends AbstractOperatorConcurrencyContext{
    /**
     * The pool used by this context
     */
    private static final LazyPool BACKGROUND_POOL = new LazyPool(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_NUMBER_OF_THREADS_BACKGROUND);

    /**
     * Creates a new {@link BackgroundConcurrencyContext} for the given {@link Operator}.
     * <p>
     * The context assumes that only operators that belong to the corresponding operator submit tasks to this context.
     *
     * @param operator
     * 		the corresponding operator
     */
    public BackgroudOperatorConcurrencyContext(Operator operator) {
        super(operator, BACKGROUND_POOL);
    }
}
