FROM openjdk:8u121-jdk
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' >/etc/timezone
ADD /target/spring-boot-istio-server-0.0.1-SNAPSHOT.jar /
ENV SERVER_PORT="18080"
ENTRYPOINT java -jar /spring-boot-istio-server-0.0.1-SNAPSHOT.jar