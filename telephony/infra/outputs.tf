output "asterisk_public_ip" {
  description = "The public IP address of the Asterisk/FreePBX server"
  value       = aws_instance.asterisk_server.public_ip
}

output "ssh_command" {
  description = "The command to SSH into the Asterisk server"
  value       = "ssh -i asterisk-key.pem admin@${aws_instance.asterisk_server.public_ip}"
}

output "freepbx_url" {
  description = "The URL to access the FreePBX web interface"
  value       = "http://${aws_instance.asterisk_server.public_ip}"
}
