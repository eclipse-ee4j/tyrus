# Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0, which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# This Source Code may also be made available under the following Secondary
# Licenses when the conditions for such availability set forth in the
# Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
# version 2 with the GNU Classpath Exception, which is available at
# https://www.gnu.org/software/classpath/license.html.
#
# SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

# Use this script to easily generate new keystores and truststores for the tests.
# It must be executed in the same folder this script exists.

#!/bin/bash

set -e

FOLDER=./
PASSWORD='asdfgh'

function clean(){
	echo "=============================> Cleaning"
	cd ${FOLDER}
	git clean -df $FOLDER && git checkout $FOLDER && git status
}

function printKeystore(){
	KEYSTORE=$1
	echo "=============================> Print ${KEYSTORE}"
	keytool -list -keystore ${FOLDER}/${KEYSTORE} -storepass ${PASSWORD} -v
}

function createKeystore(){
	NEW_ALIAS=$1
	KEYSTORE=$2
	DNAME=$3
	NEW_KEYSTORE=${KEYSTORE}
	keytool -keystore ${FOLDER}/${NEW_KEYSTORE} -genkey -keyalg RSA -alias ${NEW_ALIAS} -storepass ${PASSWORD} -dname "${DNAME}" -validity 36500
	keytool -list -rfc -keystore ${FOLDER}/${NEW_KEYSTORE} -alias ${NEW_ALIAS} -storepass ${PASSWORD} > ${FOLDER}/${NEW_ALIAS}.cert
	echo "=============================> ${FOLDER}/${OLD_KEYSTORE} was created"
}

function createTruststore(){
	NEW_ALIAS=$1
	TRUSTSTORE=$2
	CERT=$3
	keytool -import -noprompt -file ${FOLDER}/${CERT} -alias ${NEW_ALIAS} -keystore ${FOLDER}/${TRUSTSTORE} -storepass ${PASSWORD}
	echo "=============================> ${FOLDER}/${TRUSTSTORE} was created"
}

clean

createKeystore 'serverkey' 'keystore_server.new' 'CN=localhost, OU=Jersey, O=Oracle Corporation, L=Prague, ST=Czech Republic, C=CZ'
createKeystore 'clientkey' 'keystore_client.new' 'CN=Client, OU=Jersey, O=Oracle Corporation, L=Prague, ST=Czech Republic, C=CZ'
createTruststore 'clientcert' 'truststore_server.new' 'clientkey.cert'
createTruststore 'servercert' 'truststore_client.new' 'serverkey.cert'
