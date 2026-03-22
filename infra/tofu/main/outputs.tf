output "elastic_ip" {
  description = "Elastic IP address for the EC2 instance (Phase 18: Cloudflare DNS A record)"
  value       = aws_eip.main.public_ip
}

output "instance_id" {
  description = "EC2 instance ID (Phase 15: K3s installation target)"
  value       = aws_instance.main.id
}

output "instance_public_dns" {
  description = "Public DNS name of the EC2 instance (convenience for SSH access)"
  value       = aws_instance.main.public_dns
}

output "security_group_id" {
  description = "Security group ID (Phase 15 may add K3s API port 6443)"
  value       = aws_security_group.main.id
}

output "vpc_id" {
  description = "VPC ID for future networking resources"
  value       = aws_vpc.main.id
}

output "subnet_id" {
  description = "Public subnet ID for future resources"
  value       = aws_subnet.public.id
}
