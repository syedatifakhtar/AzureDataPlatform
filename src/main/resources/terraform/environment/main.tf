provider "azurerm" {
  version = "=2.11.0"
  features {}
}
terraform {
  backend "azurerm" {}
}


resource "azurerm_resource_group" "environmentrg" {
  name     = var.rg_name
  location = var.location
  tags = {
    owner = var.owner
  }
}



resource "azurerm_storage_container" "state_container" {
  name = var.env_state_container_name
  storage_account_name = var.master_storage_account_name
  container_access_type = "private"
}

