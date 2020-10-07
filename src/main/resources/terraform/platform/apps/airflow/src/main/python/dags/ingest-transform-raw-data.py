from os import environ

from airflow import DAG
from airflow.contrib.hooks.ssh_hook import SSHHook
from datetime import timedelta
from airflow.utils.dates import days_ago


from airflow import DAG
from airflow.contrib.operators.databricks_operator import DatabricksSubmitRunOperator

dag = DAG(dag_id='raw-to-parquet', default_args=None, schedule_interval=None,start_date=days_ago(2),catchup=False)

spark_jar_task = DatabricksSubmitRunOperator(
  task_id='spark_jar_task',
  dag=dag,
  existing_cluster_id='1234',
  spark_jar_task={
    'main_class_name': 'com.example.ProcessData'
  },
  libraries=[
    {
      'jar': 'dbfs:/lib/etl-0.1.jar'
    }
  ]
)