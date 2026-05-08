# Default VPC and Subnet discovery
data "aws_vpc" "default" {
  default = true
}

resource "aws_security_group" "asterisk_sg" {
  name        = "asterisk_freepbx_sg"
  description = "Security group for Asterisk/FreePBX"
  vpc_id      = data.aws_vpc.default.id

  # SSH (TCP 22) - Restrict to your IP ideally, opened to 0.0.0.0/0 for initial setup 

  ingress {
    description = "SSH Access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] 
  }

  # FreePBX Web UI (HTTP/HTTPS) - Restrict to your IP ideally
  ingress {
    description = "FreePBX HTTP Admin"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "FreePBX HTTPS Admin"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SIP Signaling (UDP 5060) - Open to Twilio IPs (North America / Default)
  ingress {
    description = "SIP Signaling from Twilio (NA)"
    from_port   = 5060
    to_port     = 5060
    protocol    = "udp"
    cidr_blocks = [
      "54.172.60.0/30",
      "54.244.51.0/30",
      "54.171.127.192/30",
      "35.156.191.128/30",
      "54.65.63.192/30",
      "54.169.127.128/30",
      "54.252.254.64/30",
      "177.71.206.192/30"
    ]
  }

  # RTP Media (UDP 10000-20000) - Open to Twilio Media IPs
  ingress {
    description = "RTP Media from Twilio"
    from_port   = 10000
    to_port     = 20000
    protocol    = "udp"
    cidr_blocks = [
      "54.172.60.0/23",
      "34.203.250.0/23",
      "54.244.51.0/24",
      "35.166.33.0/24",
      "54.171.127.192/26",
      "52.215.127.0/24",
      "35.156.191.128/26",
      "18.195.48.0/24",
      "54.65.63.192/26",
      "3.112.80.0/24",
      "54.169.127.128/26",
      "3.0.73.0/24",
      "54.252.254.64/26",
      "3.104.90.0/24",
      "177.71.206.192/26",
      "18.228.249.0/24"
    ]
  }

  # Allow all outbound traffic
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "Asterisk-FreePBX-SG"
  }
}
