/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.graphalgo.impl.triangle;

import org.immutables.value.Value;
import org.neo4j.graphalgo.annotation.Configuration;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.core.CypherMapWrapper;
import org.neo4j.graphalgo.newapi.AlgoBaseConfig;
import org.neo4j.graphalgo.newapi.GraphCreateConfig;
import org.neo4j.graphalgo.newapi.WeightConfig;

import java.util.Optional;

@ValueClass
@Configuration("BalancedTriadsConfigImpl")
public interface BalancedTriadsConfig extends AlgoBaseConfig, WeightConfig {

    @Value.Default
    default String balancedProperty() {
        return "balanced";
    }

    @Value.Default
    default String unbalancedProperty() {
        return "unbalanced";
    }

    // BalancedTriads does not use the `writeProperty` options,
    // but requires `writeConcurrency` to be present. We opted
    // for not setting `writeProperty` to some arbitrary default.
    @Value.Default
    default int writeConcurrency() {
        return concurrency();
    }

    static BalancedTriadsConfig of(
        String username,
        Optional<String> graphName,
        Optional<GraphCreateConfig> maybeImplicitCreate,
        CypherMapWrapper userInput
    ) {
        return new BalancedTriadsConfigImpl(
            graphName,
            maybeImplicitCreate,
            username,
            userInput
        );
    }

}