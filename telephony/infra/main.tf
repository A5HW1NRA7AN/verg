terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# Generate an SSH key pair
resource "tls_private_key" "asterisk_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# Upload the public key to AWS
resource "aws_key_pair" "asterisk_key_pair" {
  key_name   = var.key_name
  public_key = tls_private_key.asterisk_key.public_key_openssh
}

# Save the private key locally for SSH access
resource "local_sensitive_file" "private_key" {
  content         = tls_private_key.asterisk_key.private_key_pem
  filename        = "${path.module}/asterisk-key.pem"
  file_permission = "0400"
}

# Find the latest Debian 12 AMI (Required by FreePBX 17)
data "aws_ami" "debian_12" {
  most_recent = true
  owners      = ["136693071363"] # Debian Official

  filter {
    name   = "name"
    values = ["debian-12-amd64-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 Instance for Asterisk/FreePBX
resource "aws_instance" "asterisk_server" {
  ami           = data.aws_ami.debian_12.id
  instance_type = var.instance_type
  key_name      = aws_key_pair.asterisk_key_pair.key_name

  vpc_security_group_ids = [aws_security_group.asterisk_sg.id]

  # Allocate 20GB of disk space (gp3) to prevent 'No space left on device' errors
  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    delete_on_termination = true
  }

  # Cloud-Init script to install FreePBX 17 automatically
  user_data = <<-EOF
    #!/bin/bash
    set -e
    # Update and set hostname
    hostnamectl set-hostname freepbx.local
    apt-get update -y
    apt-get upgrade -y

    # Download and run the official Sangoma FreePBX install script
    cd /usr/src
    wget https://github.com/FreePBX/sng_freepbx_debian_install/raw/master/sng_freepbx_debian_install.sh -O sng_freepbx_debian_install.sh
    chmod +x sng_freepbx_debian_install.sh

    # Run the installation completely unattended
    # We export DEBIAN_FRONTEND so prompts are suppressed
    export DEBIAN_FRONTEND=noninteractive
    ./sng_freepbx_debian_install.sh
  EOF

  tags = {
    Name = "Asterisk-FreePBX-Server"
  }

  # Ensure the instance has a public IP
  associate_public_ip_address = true
}
