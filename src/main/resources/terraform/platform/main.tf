terraform {
  backend "local" {}
}

output "hello_world" {
  value = "Hello, World!"
}

output "print_foo" {
  value = var.foo
}

output "print_foo2" {
  value = var.foo2
}

variable "foo" {}
variable "foo2" {}