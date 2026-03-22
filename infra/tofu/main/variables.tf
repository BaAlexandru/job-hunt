variable "region" {
  description = "AWS region for all resources"
  type        = string
  default     = "eu-central-1"
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to SSH (e.g., your IP: x.x.x.x/32)"
  type        = string
}

variable "alert_email" {
  description = "Email address for billing alarm notifications"
  type        = string
}

variable "ssh_public_key_path" {
  description = "Path to SSH public key file for EC2 access"
  type        = string
  default     = "~/.ssh/jobhunt-deployer.pub"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}
