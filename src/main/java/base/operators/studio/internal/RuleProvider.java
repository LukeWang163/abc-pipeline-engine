/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package base.operators.studio.internal;

import java.io.InputStream;


/**
 * Provider for rule json
 *
 * Used by the {@link base.operators.tool.usagestats.CallToActionScheduler} as a source of
 * {@link base.operators.tool.usagestats.Rule}s.
 *
 * Must be registered with the {@link RuleProviderRegistry}.
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5
 */
public interface RuleProvider {

	/**
	 * Returns a JSON encoded array of {@link base.operators.tool.usagestats.Rule} objects
	 *
	 * @return The {@code InputStream} or {@code null}
	 */
	public InputStream getRuleJson();

}
