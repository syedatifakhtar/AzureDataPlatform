output "kube-config" {
  value = azurerm_kubernetes_cluster.aks_cluster.kube_config_raw
}

output "databricks_workspace_host" {
  value = azurerm_databricks_workspace.dp_databricks_workspace.workspace_url
}

output "databricks_workspace_id" {
  value = azurerm_databricks_workspace.dp_databricks_workspace.managed_resource_group_id
}

output "databricks_workspace_name" {
  value = azurerm_databricks_workspace.dp_databricks_workspace.managed_resource_group_name
}