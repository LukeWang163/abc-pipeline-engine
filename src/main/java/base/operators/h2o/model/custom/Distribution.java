package base.operators.h2o.model.custom;

import base.operators.h2o.model.custom.Model.Parameters;

public class Distribution {
    public static double MIN_LOG = -19.0D;
    public static double MAX = 1.0E19D;
    public final Distribution.Family distribution;
    public final double tweediePower;
    public final double quantileAlpha;

    public Distribution(Distribution.Family family) {
        this.distribution = family;

        assert family != Distribution.Family.tweedie;

        assert family != Distribution.Family.quantile;

        this.tweediePower = 1.5D;
        this.quantileAlpha = 0.5D;
    }

    public Distribution(Parameters params) {
        this.distribution = params._distribution;
        this.tweediePower = params._tweedie_power;
        this.quantileAlpha = params._quantile_alpha;

        assert this.tweediePower > 1.0D && this.tweediePower < 2.0D;
    }

    public static double exp(double x) {
        double val = Math.min(MAX, Math.exp(x));
        return val;
    }

    public static double log(double x) {
        x = Math.max(0.0D, x);
        double val = x == 0.0D ? MIN_LOG : Math.max(MIN_LOG, Math.log(x));
        return val;
    }

    public static String expString(String x) {
        return "Math.min(" + MAX + ", Math.exp(" + x + "))";
    }

    public double deviance(double w, double y, double f) {
        f = this.link(f);
        switch(this.distribution) {
            case AUTO:
            case gaussian:
                return w * (y - f) * (y - f);
            case huber:
                if (Math.abs(y - f) < 1.0D) {
                    return w * (y - f) * (y - f);
                }

                return 2.0D * w * Math.abs(y - f) - 1.0D;
            case laplace:
                return w * Math.abs(y - f);
            case quantile:
                return y > f ? w * this.quantileAlpha * (y - f) : w * (1.0D - this.quantileAlpha) * (f - y);
            case bernoulli:
                return -2.0D * w * (y * f - log(1.0D + exp(f)));
            case poisson:
                return -2.0D * w * (y * f - exp(f));
            case gamma:
                return 2.0D * w * (y * exp(-f) + f);
            case tweedie:
                assert this.tweediePower > 1.0D && this.tweediePower < 2.0D;

                return 2.0D * w * (Math.pow(y, 2.0D - this.tweediePower) / ((1.0D - this.tweediePower) * (2.0D - this.tweediePower)) - y * exp(f * (1.0D - this.tweediePower)) / (1.0D - this.tweediePower) + exp(f * (2.0D - this.tweediePower)) / (2.0D - this.tweediePower));
            default:
                throw H2O.unimpl();
        }
    }

    public double gradient(double y, double f) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case bernoulli:
            case poisson:
                return y - this.linkInv(f);
            case huber:
                if (Math.abs(y - f) < 1.0D) {
                    return y - f;
                }

                return f - 1.0D >= y ? -1.0D : 1.0D;
            case laplace:
                return f > y ? -1.0D : 1.0D;
            case quantile:
                return y > f ? this.quantileAlpha : this.quantileAlpha - 1.0D;
            case gamma:
                return y * exp(-f) - 1.0D;
            case tweedie:
                assert this.tweediePower > 1.0D && this.tweediePower < 2.0D;

                return y * exp(f * (1.0D - this.tweediePower)) - exp(f * (2.0D - this.tweediePower));
            default:
                throw H2O.unimpl();
        }
    }

    public double link(double f) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case huber:
            case laplace:
            case quantile:
                return f;
            case bernoulli:
                return log(f / (1.0D - f));
            case poisson:
            case gamma:
            case tweedie:
            case multinomial:
                return log(f);
            default:
                throw H2O.unimpl();
        }
    }

    public double linkInv(double f) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case huber:
            case laplace:
            case quantile:
                return f;
            case bernoulli:
                return 1.0D / (1.0D + exp(-f));
            case poisson:
            case gamma:
            case tweedie:
            case multinomial:
                return exp(f);
            default:
                throw H2O.unimpl();
        }
    }

    public String linkInvString(String f) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case huber:
            case laplace:
            case quantile:
                return f;
            case bernoulli:
                return "1/(1+" + expString("-" + f) + ")";
            case poisson:
            case gamma:
            case tweedie:
            case multinomial:
                return expString(f);
            default:
                throw H2O.unimpl();
        }
    }

    public double initFNum(double w, double o, double y) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case bernoulli:
            case multinomial:
                return w * (y - o);
            case huber:
            case laplace:
            case quantile:
            default:
                throw H2O.unimpl();
            case poisson:
                return w * y;
            case gamma:
                return w * y * this.linkInv(-o);
            case tweedie:
                return w * y * exp(o * (1.0D - this.tweediePower));
        }
    }

    public double initFDenom(double w, double o) {
        switch(this.distribution) {
            case AUTO:
            case gaussian:
            case bernoulli:
            case gamma:
            case multinomial:
                return w;
            case huber:
            case laplace:
            case quantile:
            default:
                throw H2O.unimpl();
            case poisson:
                return w * this.linkInv(o);
            case tweedie:
                return w * exp(o * (2.0D - this.tweediePower));
        }
    }

    public double gammaNum(double w, double y, double z, double f) {
        switch(this.distribution) {
            case gaussian:
            case bernoulli:
            case multinomial:
                return w * z;
            case huber:
            case laplace:
            case quantile:
            default:
                throw H2O.unimpl();
            case poisson:
                return w * y;
            case gamma:
                return w * (z + 1.0D);
            case tweedie:
                return w * y * exp(f * (1.0D - this.tweediePower));
        }
    }

    public double gammaDenom(double w, double y, double z, double f) {
        switch(this.distribution) {
            case gaussian:
            case gamma:
                return w;
            case huber:
            case laplace:
            case quantile:
            default:
                throw H2O.unimpl();
            case bernoulli:
                double ff = y - z;
                return w * ff * (1.0D - ff);
            case poisson:
                return w * (y - z);
            case tweedie:
                return w * exp(f * (2.0D - this.tweediePower));
            case multinomial:
                double absz = Math.abs(z);
                return w * absz * (1.0D - absz);
        }
    }

    public static enum Family {
        AUTO,
        bernoulli,
        multinomial,
        gaussian,
        poisson,
        gamma,
        tweedie,
        huber,
        laplace,
        quantile;

        private Family() {
        }
    }
}
