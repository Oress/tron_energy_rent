FROM openjdk:8-jdk

RUN git clone https://github.com/tron-nile-testnet/nile-testnet.git
RUN git clone https://github.com/tronprotocol/event-plugin.git

WORKDIR ./event-plugin
RUN chmod +x ./gradlew && ./gradlew clean build -x test

WORKDIR ../nile-testnet
RUN chmod +x ./gradlew && ./gradlew clean build -x test


WORKDIR ./build/libs
RUN mkdir output-directory

VOLUME /nile-testnet/build/libs/output-directory

EXPOSE 5555

# CMD sleep 60
ENTRYPOINT nohup java -Xms9G -Xmx9G -XX:ReservedCodeCacheSize=256m \
             -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m \
             -XX:MaxDirectMemorySize=1G -XX:+PrintGCDetails \
             -XX:+PrintGCDateStamps  -Xloggc:gc.log \
             -XX:+UseConcMarkSweepGC -XX:NewRatio=2 \
             -XX:+CMSScavengeBeforeRemark -XX:+ParallelRefProcEnabled \
             -XX:+HeapDumpOnOutOfMemoryError \
             -XX:+UseCMSInitiatingOccupancyOnly  -XX:CMSInitiatingOccupancyFraction=70 \
             -jar FullNode.jar -c config.conf --es >> start.log 2>&1
