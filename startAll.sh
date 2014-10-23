echo Setting Time...
sudo ntpdate -s time.nist.gov
ssh solr@localhost "cd /opt/solr/solr/iicaptain ;nohup java -jar start.jar >solr.out 2>solr.err </dev/null &"
FLUME_HOME=/usr/lib/flume
echo Starting Flume Agent writing events to HBase...
/usr/bin/flume-ng agent -Xmx256m --conf conf --conf-file /opt/flume/flumeToHBase.conf --name a1 -Dflume.root.logger=INFO,console &
sleep 10
echo Starting Derby...
cd /opt/iicaptainDB 
../db-derby-10.10.1.1-bin/bin/startNetworkServer  -h tomcatvm &
cd /opt
sleep 10 
echo Starting Flume ingesting events from Tomcat...
$FLUME_HOME/bin/flume-ng agent -Xmx256m --conf conf --conf-file /opt/flume/tomcatToFlume.conf --name a1 -Dflume.root.logger=INFO,console &
sleep 10
echo Starting Kafka... 
cd ~
rm -fr /tmp/kafka-logs/*
/usr/lib/zookeeper/bin/zkCli.sh set /kafkastorm/src/iicaptain/partition_0 "{'topology':{'id':'4e00c80d-1eef-4f27-b656-e6a2c0c35831','name':'iicaptain-topology'},'offset':0,'partition':0,'broker':{'host':'sandbox.hortonworks.com','port':9092},'topic':'iicaptain'}"
/usr/lib/zookeeper/bin/zkCli.sh set /kafkastorm/src/iicaptain/partition_1 "{'topology':{'id':'0ceb86c3-c516-4d38-beb8-876654eb8d05','name':'iicaptain-topology'},'offset':0,'partition':1,'broker':{'host':'sandbox.hortonworks.com','port':9092},'topic':'iicaptain'}"
/opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties &
sleep 10
echo Starting Tomcat
rm /opt/tomcat/logs/*
/opt/tomcat/bin/catalina.sh start &


