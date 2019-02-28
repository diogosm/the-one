#! /bin/sh
java -Xmx512M -Djava.util.Arrays.useLegacyMergeSort=true -cp target:lib/ECLA.jar:lib/DTNConsoleConnection.jar:lib/jung-jai-2.0.1.jar:lib/jung-graph-impl-2.0.1.jar:lib/gs-core-1.3.jar:lib/gs-algo-1.3.jar core.DTNSim $*
