[//]: # " Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved. "
[//]: # "  "
[//]: # " This program and the accompanying materials are made available under the "
[//]: # " terms of the Eclipse Public License v. 2.0, which is available at "
[//]: # " http://www.eclipse.org/legal/epl-2.0. "
[//]: # "  "
[//]: # " This Source Code may also be made available under the following Secondary "
[//]: # " Licenses when the conditions for such availability set forth in the "
[//]: # " Eclipse Public License v. 2.0 are satisfied: GNU General Public License, "
[//]: # " version 2 with the GNU Classpath Exception, which is available at "
[//]: # " https://www.gnu.org/software/classpath/license.html. "
[//]: # "  "
[//]: # " SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0 "

This module is to run the TCK tests.

The module can check Tyrus 2.2.x versions.

Usage:
 * Download the TCK bundle from https://download.eclipse.org/ee4j/jakartaee-tck/jakartaee11/staged/eftl/
 * Unzip the bundle and install the module from the bundle (for instance):
     * mvn install:install-file -Dfile=./websocket-tck-spec-tests-2.2.0.jar -DgroupId=jakarta.tck -DartifactId=websocket-tck-spec-tests -Dversion=2.2.0 -Dpackaging=jar
 * Download javatest from https://github.com/jakartaee/platform-tck/blob/master/lib/javatest.jar and install:  
     * mvn install:install-file -Dfile=lib/javatest.jar -DgroupId=javatest -DartifactId=javatest -Dversion=5.0 -Dpackaging=jar
 * Run the test:
     * mvn clean install -Dtyrus.version=<version> [-Pstaging]
