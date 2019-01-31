#! /bin/sh
java -Xmx512M -cp target:lib/ECLA.jar:lib/DTNConsoleConnection.jar:lib/jung-jai-2.0.1.jar:lib/jung-graph-impl-2.0.1.jar core.DTNSim $*
