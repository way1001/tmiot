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
FROM way/base-jre:2025.2 AS runtime
LABEL base.author=way
LABEL base.author.email=ways.icloud.com

# default env
ENV PARAMS=''
ENV NODE_ENV=test
ENV SERVER_PORT=8500
ENV SERVER_NAME=base-center-data
ENV SERVER_PACKAGES=com.aiforest.tmiot
ENV APM_AGENT_ENABLE=false
ENV APM_SERVICE=http://base-apm:9300
ENV GRPC_PORT=9500

# default jvm & gc env
ENV JAVA_OPS="-server \
              -Xms256m \
              -Xmx512m \
              -XX:+UseG1GC \
              -Djava.security.egd=file:/dev/./urandom"
ENV GC_LOG="-Xlog:gc*:file=base/logs/center/data/gc/gc.log:time,uptime,level,tags:filecount=20,filesize=10M"

WORKDIR /base-center/base-center-data

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && mkdir -p /base-center/base-center-data/base/logs/center/data/gc

COPY target/base-center-data.jar ./

RUN cp /usr/share/base/entrypoint.sh ./entrypoint.sh

EXPOSE ${SERVER_PORT}
EXPOSE ${GRPC_PORT}
VOLUME /base-center/base-center-data/base/logs

ENTRYPOINT ["./entrypoint.sh"]

CMD ["base-center-data.jar"]
