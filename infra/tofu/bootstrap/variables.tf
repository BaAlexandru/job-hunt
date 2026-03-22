variable "region" {
  description = "AWS region for the state bucket"
  type        = string
  default     = "eu-central-1"
}

variable "bucket_name" {
  description = "Name of the S3 bucket for OpenTofu state"
  type        = string
  default     = "jobhunt-tofu-state"
}
