FROM puckel/docker-airflow:1.10.4

USER root

ARG AIRFLOW_HOME=/usr/local/airflow
ENV AIRFLOW_HOME=${AIRFLOW_HOME}
RUN mkdir -p /usr/share/man/man1 && \
    apt-get update && \
    apt-get install -y openjdk-8-jre && \
    pip install awscli && \
    apt-get install -y vim jq && \
    pip install boto3 && \
    pip install flask-bcrypt

COPY src/main/python/dags /usr/local/airflow/dags/
COPY src/main/python/configs /usr/local/airflow/configs/
COPY src/main/python/airflow.cfg ${AIRFLOW_HOME}/airflow.cfg
COPY entrypoint.sh /
RUN chmod +x /entrypoint.sh
USER airflow

ENTRYPOINT ["/entrypoint.sh"]
CMD ["webserver"]
