#!/bin/bash

DIR=`dirname $0`

java -cp $DIR:$DIR/build:$DIR/m_agent_service/m_agent_service-1.0.jar:$DIR/m_agent_service/stanford-parser-2.0.4-models.jar:$DIR/clausie_lib/jopt-simple-4.4.jar de.mpii.clausie.ClausIE $*
