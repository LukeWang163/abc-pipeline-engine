package base.operators.operator.etl.trans;

public class Counter {
    private double counter;
    private double start;
    private double increment;
    private double maximum;
    private boolean loop;

    public Counter() {
        start = 1L;
        increment = 1L;
        maximum = 0L;
        loop = false;
        counter = start;
    }

    public Counter( double start ) {
        this();
        this.start = start;
        counter = start;
    }

    public Counter( double start, double increment ) {
        this( start );
        this.increment = increment;
    }

    public Counter( double start, double increment, double maximum ) {
        this( start, increment );
        this.loop = true;
        this.maximum = maximum;
    }

    /**
     * @return Returns the counter.
     */
    public double getCounter() {
        return counter;
    }

    /**
     * @return Returns the increment.
     */
    public double getIncrement() {
        return increment;
    }

    /**
     * @return Returns the maximum.
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * @return Returns the start.
     */
    public double getStart() {
        return start;
    }

    /**
     * @return Returns the loop.
     */
    public boolean isLoop() {
        return loop;
    }

    /**
     * @param counter
     *          The counter to set.
     */
    public void setCounter( double counter ) {
        this.counter = counter;
    }

    /**
     * @param increment
     *          The increment to set.
     */
    public void setIncrement( double increment ) {
        this.increment = increment;
    }

    /**
     * @param loop
     *          The loop to set.
     */
    public void setLoop( boolean loop ) {
        this.loop = loop;
    }

    /**
     * @param maximum
     *          The maximum to set.
     */
    public void setMaximum( double maximum ) {
        this.maximum = maximum;
    }

    public double next() {
        double retval = counter;

        counter += increment;
        if ( loop && counter > maximum ) {
            counter = start;
        }

        return retval;
    }
}
