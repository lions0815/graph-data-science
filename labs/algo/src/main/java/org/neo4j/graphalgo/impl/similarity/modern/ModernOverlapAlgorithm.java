/*
 * Copyright (c) 2017-2019 "Neo4j,"
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

package org.neo4j.graphalgo.impl.similarity.modern;

import org.neo4j.graphalgo.impl.similarity.CategoricalInput;
import org.neo4j.graphalgo.impl.similarity.RleDecoder;
import org.neo4j.graphalgo.impl.similarity.SimilarityComputer;
import org.neo4j.graphalgo.impl.similarity.SimilarityInput;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModernOverlapAlgorithm extends ModernSimilarityAlgorithm<ModernOverlapAlgorithm, CategoricalInput> {

    public ModernOverlapAlgorithm(ModernSimilarityConfig config, GraphDatabaseAPI api) {
        super(config, api);
    }

    @Override
    CategoricalInput[] prepareInputs(Object rawData, ModernSimilarityConfig config) {
        return prepareCategories((List<Map<String, Object>>) rawData, config.degreeCutoff());
    }

    private CategoricalInput[] prepareCategories(List<Map<String, Object>> data, long degreeCutoff) {
        CategoricalInput[] ids = new CategoricalInput[data.size()];
        int idx = 0;
        for (Map<String, Object> row : data) {
            List<Number> targetIds = SimilarityInput.extractValues(row.get("categories"));
            int size = targetIds.size();
            if (size > degreeCutoff) {
                long[] targets = new long[size];
                int i = 0;
                for (Number id : targetIds) {
                    targets[i++] = id.longValue();
                }
                Arrays.sort(targets);
                ids[idx++] = new CategoricalInput((Long) row.get("item"), targets);
            }
        }
        if (idx != ids.length) ids = Arrays.copyOf(ids, idx);
        Arrays.sort(ids);
        return ids;
    }

    @Override
    SimilarityComputer<CategoricalInput> similarityComputer(
        Double skipValue,
        int[] sourceIndexIds,
        int[] targetIndexIds
    ) {
        if (sourceIndexIds.length > 0 || targetIndexIds.length > 0) {
            return (decoder, s, t, cutoff) -> s.overlap(cutoff, t, false);
        }
        return (decoder, s, t, cutoff) -> s.overlap(cutoff, t);
    }

    @Override
    Supplier<RleDecoder> inputDecoderFactory(CategoricalInput[] inputs) {
        return () -> null;
    }
}
