provider "azurerm" {
  version = "=2.11.0"
  features {}
}

terraform {
  backend "azurerm" {}
}

resource "azurerm_virtual_network" "platform-net" {
  name                = "platform-net"
  resource_group_name = var.parent_resource_group_name
  location            = var.location
  address_space       = ["10.1.0.0/16"]
}

resource "azurerm_network_security_group" "databricks_private_nsg" {
  name                = "privateDatabricksNSG"
  location            = var.location
  resource_group_name = var.parent_resource_group_name

  tags = {
    DEPLOYMENT_IDENTIFIER = var.deployment_identifier
    OWNER = "platform"
  }
}

resource "azurerm_subnet" "databricks_private" {
  name                 = "databricks_private_subnet"
  resource_group_name  = var.parent_resource_group_name
  virtual_network_name = azurerm_virtual_network.platform-net.name
  address_prefixes = ["10.1.0.0/20"]

  delegation {
    name = "databricks-del-public"

    service_delegation {
      name = "Microsoft.Databricks/workspaces"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
        "Microsoft.Network/virtualNetworks/subnets/join/action",
        "Microsoft.Network/virtualNetworks/subnets/prepareNetworkPolicies/action",
      ]
    }
  }
}

resource "azurerm_subnet_network_security_group_association" "databricks_private_subnet_association" {
  subnet_id                 = azurerm_subnet.databricks_private.id
  network_security_group_id = azurerm_network_security_group.databricks_private_nsg.id
}

resource "azurerm_subnet_network_security_group_association" "databricks_public_subnet_association" {
  subnet_id                 = azurerm_subnet.databricks_public.id
  network_security_group_id = azurerm_network_security_group.databricks_private_nsg.id
}

resource "azurerm_subnet" "databricks_public" {
  name                 = "databricks_public_subnet"
  resource_group_name  = var.parent_resource_group_name
  virtual_network_name = azurerm_virtual_network.platform-net.name
  address_prefixes = ["10.1.16.0/20"]

  delegation {
    name = "databricks-del-public"

    service_delegation {
      name = "Microsoft.Databricks/workspaces"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
        "Microsoft.Network/virtualNetworks/subnets/join/action",
        "Microsoft.Network/virtualNetworks/subnets/prepareNetworkPolicies/action"
      ]
    }
  }
}

resource "azurerm_subnet" "aks" {
  name                 = "akssubnet"
  resource_group_name  = var.parent_resource_group_name
  virtual_network_name = azurerm_virtual_network.platform-net.name
  address_prefixes = ["10.1.32.0/19"]
}


resource "azurerm_databricks_workspace" "dp_databricks_workspace" {
  name                = "db-workspace-main"
  resource_group_name = var.parent_resource_group_name
  location            = var.location
  sku                 = "premium"
  custom_parameters {
    virtual_network_id = azurerm_virtual_network.platform-net.id
    private_subnet_name = azurerm_subnet.databricks_private.name
    public_subnet_name = azurerm_subnet.databricks_public.name
  }


tags = {
    DEPLOYMENT_IDENTIFIER = var.deployment_identifier
    OWNER = "platform"
  }
}

resource "azurerm_kubernetes_cluster" "aks_cluster" {
  name                = var.aks_cluster_name
  location            = var.location
  resource_group_name = var.parent_resource_group_name
  dns_prefix          = var.aks_cluster_dns_prefix

  tags = {
    DEPLOYMENT_IDENTIFIER = var.deployment_identifier
  }

  role_based_access_control {
    enabled = true
  }

  network_profile {
    network_plugin    = "azure"
    load_balancer_sku = "Standard"

  }
//  private_cluster_enabled = true
  service_principal {
    client_id = var.client_id
    client_secret = var.client_secret
  }
  # New default_node_pool
  default_node_pool {
    availability_zones    = []
    enable_auto_scaling   = false
    enable_node_public_ip = false
    max_pods              = 10
    name                  = "default"
    node_count            = 3
    node_taints           = []
    os_disk_size_gb       = 50
    type                  = "AvailabilitySet"
    vm_size               = "Standard_DS4_v2"
    vnet_subnet_id      = azurerm_subnet.aks.id
  }
}

output "client_certificate" {
  value = azurerm_kubernetes_cluster.aks_cluster.kube_config.0.client_certificate
}

output "kube_config" {
  value = azurerm_kubernetes_cluster.aks_cluster.kube_config_raw
}