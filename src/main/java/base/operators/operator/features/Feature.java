//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package base.operators.operator.features;

import base.operators.example.Attribute;
import java.io.Serializable;

public class Feature implements Serializable {
    private static final long serialVersionUID = -5812032201995322773L;
    private String name;
    private String constructionExpression;
    private boolean numerical;
    private int complexity;

    public Feature(String name, boolean numerical) {
        this(name, "[" + name + "]", numerical, 1);
    }

    public Feature(Attribute attribute) {
        this(attribute.getName(), "[" + attribute.getName() + "]", attribute.isNumerical(), 1);
    }

    public Feature(String name, String constructionExpression, boolean numerical, int complexity) {
        this.name = name;
        this.constructionExpression = constructionExpression;
        this.numerical = numerical;
        this.complexity = complexity;
    }

    public Feature(Feature other) {
        this.name = other.name;
        this.constructionExpression = other.constructionExpression;
        this.numerical = other.numerical;
        this.complexity = other.complexity;
    }

    public String getName() {
        return this.name;
    }

    public String getExpression() {
        return this.constructionExpression;
    }

    public int getComplexity() {
        return this.complexity;
    }

    public boolean isNumerical() {
        return this.numerical;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Feature)) {
            return false;
        } else {
            Feature other = (Feature)o;
            return this.name.equals(other.name) && this.constructionExpression.equals(other.constructionExpression);
        }
    }

    @Override
    public int hashCode() {
        return (this.name + "_" + this.constructionExpression).hashCode();
    }

    @Override
    public String toString() {
        return this.name + " = " + this.constructionExpression + " {" + this.complexity + "}";
    }
}
