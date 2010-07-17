package sample.performance;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import org.eclipse.test.internal.performance.PerformanceTestPlugin;
import org.eclipse.test.internal.performance.db.DB;
import org.eclipse.test.internal.performance.db.Scenario;
import org.eclipse.test.internal.performance.db.Variations;

/**
 * Dumps performance data to stdout.
 */
public class View
{

    public static void main( String[] args )
    {

        Variations variations = PerformanceTestPlugin.getVariations();
        //variations.put( "config", "eclipseperfwin2_R3.3" ); //$NON-NLS-1$//$NON-NLS-2$
        variations.put( "build", "%" ); //$NON-NLS-1$//$NON-NLS-2$
        //variations.put( "jvm", "sun" ); //$NON-NLS-1$//$NON-NLS-2$

        String scenarioPattern = "%"; //$NON-NLS-1$

        String seriesKey = PerformanceTestPlugin.BUILD;

        String outFile = null;
        // outfile= "/tmp/dbdump"; //$NON-NLS-1$
        PrintStream ps = null;
        if ( outFile != null )
        {
            try
            {
                ps = new PrintStream( new BufferedOutputStream( new FileOutputStream( outFile ) ) );
            }
            catch ( FileNotFoundException e )
            {
                System.err.println( "can't create output file" ); //$NON-NLS-1$
            }
        }
        if ( ps == null )
            ps = System.out;

        Scenario[] scenarios = DB.queryScenarios( variations, scenarioPattern, seriesKey, null );
        ps.println( scenarios.length + " Scenarios" ); //$NON-NLS-1$
        ps.println();

        for ( int s = 0; s < scenarios.length; s++ )
            scenarios[s].dump( ps, PerformanceTestPlugin.BUILD );

        if ( ps != System.out )
            ps.close();
    }
}
