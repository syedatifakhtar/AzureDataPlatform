terraform {
  backend "local" {}
}

provider "azurerm" {
  version = "=2.11.0"
  features {}
}

resource "azurerm_resource_group" "master_rg" {
  location = var.location
  name = var.master_rg_name
}
resource "azurerm_storage_account" "master_statebucket" {
  name                     = var.master_storage_account_name
  resource_group_name      = azurerm_resource_group.master_rg.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "GRS"

  tags = {
    environment = "datamesh"
    owner = var.owner
  }
}


resource "azurerm_storage_container" "master_state_container" {
  name = var.master_tfstate_container_name
  storage_account_name = azurerm_storage_account.master_statebucket.name
  container_access_type = "private"
}
