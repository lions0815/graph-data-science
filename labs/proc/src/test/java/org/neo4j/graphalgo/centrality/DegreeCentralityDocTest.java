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

package org.neo4j.graphalgo.centrality;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.graphalgo.BaseProcTest;
import org.neo4j.graphalgo.GetNodeFunc;
import org.neo4j.graphalgo.GraphLoadProc;
import org.neo4j.graphalgo.TestDatabaseCreator;
import org.neo4j.graphalgo.core.loading.GraphCatalog;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.internal.kernel.api.exceptions.KernelException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DegreeCentralityDocTest extends BaseProcTest {

    @BeforeEach
    void setupGraph() throws KernelException {
        db = TestDatabaseCreator.createTestDatabase(builder ->
            builder.setConfig(GraphDatabaseSettings.procedure_unrestricted, "algo.*")
        );

        final String cypherUnweighted = "CREATE" +
                              "  (nAlice:User {id:'Alice'})\n" +
                              ", (nBridget:User {id:'Bridget'})\n" +
                              ", (nCharles:User {id:'Charles'})\n" +
                              ", (nDoug:User {id:'Doug'})\n" +
                              ", (nMark:User {id:'Mark'})\n" +
                              ", (nMichael:User {id:'Michael'})\n" +
                              ", (nAlice)-[:FOLLOWS {score: 1}]->(nDoug)\n" +
                              ", (nAlice)-[:FOLLOWS {score: 2}]->(nBridget)\n" +
                              ", (nAlice)-[:FOLLOWS {score: 5}]->(nCharles)\n" +
                              ", (nMark)-[:FOLLOWS {score: 1.5}]->(nDoug)\n" +
                              ", (nMark)-[:FOLLOWS {score: 4.5}]->(nMichael)\n" +
                              ", (nBridget)-[:FOLLOWS {score: 1.5}]->(nDoug)\n" +
                              ", (nCharles)-[:FOLLOWS {score: 2}]->(nDoug)\n" +
                              ", (nMichael)-[:FOLLOWS {score: 1.5}]->(nDoug)";

        registerProcedures(DegreeCentralityProc.class, GraphLoadProc.class);
        registerFunctions(GetNodeFunc.class);
        runQuery(cypherUnweighted);
    }

    @AfterEach
    void clearCommunities() {
        db.shutdown();
        GraphCatalog.removeAllLoadedGraphs();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("streamUnweightedTuples")
    void testUnweightedStreaming(String projection, String expected) {
        String query =
            "CALL gds.alpha.degree.stream({" +
            "   nodeProjection: 'User', " +
            "   relationshipProjection: {" +
            "       FOLLOWS: {" +
            "           type: 'FOLLOWS'," +
            "           projection: '" + projection + "'" +
            "       }" +
            "   }" +
            "})" +
            "YIELD nodeId, score " +
            "RETURN algo.asNode(nodeId).id AS name, score AS followers " +
            "ORDER BY followers DESC";
        String actual = runQuery(query).resultAsString();

        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("streamWeightedTuples")
    void testWeightedStreaming(String projection, String expected) {
        String query =
            "CALL gds.alpha.degree.stream({" +
            "   nodeProjection: 'User', " +
            "   relationshipProjection: {" +
            "       FOLLOWS: {" +
            "           type: 'FOLLOWS'," +
            "           projection: '" + projection + "'," +
            "           properties: {" +
            "               score: {" +
            "                   property: 'score'" +
            "               }" +
            "           }" +
            "       }" +
            "   }," +
            "   weightProperty: 'score'" +
            "})" +
            "YIELD nodeId, score " +
            "RETURN algo.asNode(nodeId).id AS name, score AS weightedFollowers " +
            "ORDER BY weightedFollowers DESC";
        String actual = runQuery(query).resultAsString();

        assertEquals(expected, actual);
    }

    @Disabled("Fails with NPE when loading with 'reverse'")
    @ParameterizedTest(name = "{0}")
    @MethodSource("writeUnweightedTuples")
    void testUnweightedWriting(String projection, String expected) {
        String query =
            "CALL gds.alpha.degree.write({" +
            "   nodeProjection: 'User', " +
            "   relationshipProjection: {" +
            "       FOLLOWS: {" +
            "           type: 'FOLLOWS'," +
            "           projection: '" + projection + "'" +
            "       }" +
            "   }," +
            "   writeProperty: 'following'" +
            "})" +
            "YIELD nodes, writeProperty";

        String actual = runQuery(query).resultAsString();

        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writeTuples")
    void testWeightedWriting(String projection, String expected) {
        String query =
            "CALL gds.alpha.degree.write({" +
            "   nodeProjection: 'User', " +
            "   relationshipProjection: {" +
            "       FOLLOWS: {" +
            "           type: 'FOLLOWS'," +
            "           projection: '" + projection + "'," +
            "           properties: {" +
            "               score: {" +
            "                   property: 'score'" +
            "               }" +
            "           }" +
            "       }" +
            "   }," +
            "   weightProperty: 'score'," +
            "   writeProperty: 'following'" +
            "})" +
            "YIELD nodes, writeProperty";
        String actual = runQuery(query).resultAsString();

        assertEquals(expected, actual);
    }

    static Stream<Arguments> streamUnweightedTuples() {
        String result = "+-----------------------+\n" +
                        "| name      | followers |\n" +
                        "+-----------------------+\n" +
                        "| \"Alice\"   | 3.0       |\n" +
                        "| \"Mark\"    | 2.0       |\n" +
                        "| \"Bridget\" | 1.0       |\n" +
                        "| \"Charles\" | 1.0       |\n" +
                        "| \"Michael\" | 1.0       |\n" +
                        "| \"Doug\"    | 0.0       |\n" +
                        "+-----------------------+\n" +
                        "6 rows\n";
        return Stream.of(
            arguments("natural", result)
            // TODO: reverse direction
        );
    }

    static Stream<Arguments> streamWeightedTuples() {
        String result = "+-------------------------------+\n" +
                        "| name      | weightedFollowers |\n" +
                        "+-------------------------------+\n" +
                        "| \"Alice\"   | 8.0               |\n" +
                        "| \"Mark\"    | 6.0               |\n" +
                        "| \"Charles\" | 2.0               |\n" +
                        "| \"Bridget\" | 1.5               |\n" +
                        "| \"Michael\" | 1.5               |\n" +
                        "| \"Doug\"    | 0.0               |\n" +
                        "+-------------------------------+\n" +
                        "6 rows\n";
        return Stream.of(
            arguments("natural", result)
            // TODO: reverse direction
        );
    }

    static Stream<Arguments> writeTuples() {
        String result = "+-----------------------+\n" +
                        "| nodes | writeProperty |\n" +
                        "+-----------------------+\n" +
                        "| 6     | \"following\"   |\n" +
                        "+-----------------------+\n" +
                        "1 row\n";
        return Stream.of(
            arguments("natural", result)
            // TODO: reverse direction
        );
    }

}
