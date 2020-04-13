FROM openjdk:8u121-jdk
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' >/etc/timezone
ADD /target/spring-boot-istio-client-0.0.1-SNAPSHOT.jar /
ENV GRPC_SERVER_HOST="spring-boot-istio-server"
ENV GRPC_SERVER_PORT="18888"
ENTRYPOINT java -jar /spring-boot-istio-client-0.0.1-SNAPSHOT.jar \
 --server-host=$GRPC_SERVER_HOST \
 --server-port=$GRPC_SERVER_PORT
