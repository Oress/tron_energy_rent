FROM openjdk:8-jdk

RUN git clone https://github.com/tronprotocol/java-tron.git
WORKDIR ./java-tron
RUN chmod +x ./gradlew && ./gradlew clean build -x test


WORKDIR ./build/libs
RUN mkdir output-directory

EXPOSE 5555
EXPOSE 8090

# CMD sleep 60
ENTRYPOINT nohup java -Xmx24g -XX:+PrintGCDetails \
             -XX:+PrintGCDateStamps  -Xloggc:gc.log \
             -XX:+UseConcMarkSweepGC -XX:+HeapDumpOnOutOfMemoryError \
             -jar FullNode.jar -c config.conf --es