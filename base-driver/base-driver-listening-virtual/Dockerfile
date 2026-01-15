#
# Copyright 2016-present the TM IoT original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# runtime
FROM way/tmiot-jre:2025.2 AS runtime
LABEL tmiot.author=way
LABEL tmiot.author.email=ways.icloud.com

# default env
ENV PARAMS=''
ENV NODE_ENV=test
ENV SERVER_NAME=tmiot-driver-listening-virtual
ENV SERVER_PACKAGES=com.aiforest.tmiot
ENV APM_AGENT_ENABLE=false
ENV APM_SERVICE=http://tmiot-apm:9300
ENV TCP_PORT 6270
ENV UDP_PORT 6271

# default jvm & gc env
ENV JAVA_OPS="-server \
              -Xms256m \
              -Xmx512m \
              -XX:+UseG1GC \
              -Djava.security.egd=file:/dev/./urandom"
ENV GC_LOG="-Xlog:gc*:file=tmiot/logs/driver/listening-virtual/gc/gc.log:time,uptime,level,tags:filecount=20,filesize=10M"

WORKDIR /tmiot-driver/tmiot-driver-listening-virtual

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /tmiot-driver/tmiot-driver-listening-virtual/tmiot/logs/driver/listening-virtual/gc

COPY target/tmiot-driver-listening-virtual.jar ./

RUN cp /usr/share/tmiot/entrypoint.sh ./entrypoint.sh

EXPOSE ${TCP_PORT}
EXPOSE ${UDP_PORT}
VOLUME /tmiot-driver/tmiot-driver-listening-virtual/tmiot/logs

ENTRYPOINT ["./entrypoint.sh"]

CMD ["tmiot-driver-listening-virtual.jar"]
