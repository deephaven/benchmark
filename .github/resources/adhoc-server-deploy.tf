terraform {
  required_providers {
    pnap = {
      source  = "phoenixnap/pnap"
      version = "=0.29.0"
    }
  }
}

provider "pnap" {
  client_id     = var.client_id
  client_secret = var.client_secret
}

variable "client_id" {}
variable "client_secret" {}
variable "hostname" {}
variable "plan" {}

resource "pnap_server" "adhoc_server" {
  hostname    = var.hostname
  description = "Ephemeral adhoc benchmark server"
  os          = "ubuntu/noble"
  type        = var.plan
  location    = "CHI"
  install_default_ssh_keys = true
  
  network_configuration {
    public_network_configuration {}
    private_network_configuration {}
    ip_blocks_configuration {}
  }

  esxi {
    datastore_configuration {}
  }

  netris_softgate {}

  cloud_init {
    user_data = file("${path.module}/adhoc-server-init.yml")
  }

  tags {
    tag_assignment {
      name  = "ephemeral"
      value = "true"
    }
  }

  tags {
    tag_assignment {
      name  = "created"
      value = timestamp()
    }
  }
}

output "public_ip" {
  value = tolist(pnap_server.adhoc_server.public_ip_addresses)[0]
}

output "server_id" {
  value = pnap_server.adhoc_server.id
}

