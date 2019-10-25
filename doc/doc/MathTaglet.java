/**
 * Copyright (C) 2001-2019 RapidMiner GmbH
 */
package base.operators.doc;

import java.util.Map;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;

/**
 * A taglet with name &quot;@operators.math&quot; can be used in the Javadoc comments of an operator to produce mathematical
 * code. Example: &quot;@operators.math 1/(n+1)&quot;. This will include a LaTeX math environment to you documentation.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class MathTaglet implements TexTaglet {

	private static final String NAME = "operators.math";

	public String getName() {
		return NAME;
	}

	public boolean inField() {
		return true;
	}

	public boolean inConstructor() {
		return true;
	}

	public boolean inMethod() {
		return true;
	}

	public boolean inOverview() {
		return true;
	}

	public boolean inPackage() {
		return true;
	}

	public boolean inType() {
		return true;
	}

	public boolean isInlineTag() {
		return true;
	}

	public static void register(Map<String, Taglet> tagletMap) {
		MathTaglet tag = new MathTaglet();
		Taglet t = tagletMap.get(tag.getName());
		if (t != null) {
			tagletMap.remove(tag.getName());
		}
		tagletMap.put(tag.getName(), tag);
	}

	public String toString(Tag tag) {
		return "<i>" + tag.text() + "</i>";
	}

	public String toString(Tag[] tags) {
		return null;
	}

	public String toTex(Tag tag) {
		return "$" + tag.text() + "$";
	}

	public String toTex(Tag[] tag) {
		return null;
	}
}
