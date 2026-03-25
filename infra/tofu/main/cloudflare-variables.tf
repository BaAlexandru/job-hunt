variable "cloudflare_api_token" {
  description = "Cloudflare API token (Zone:DNS:Edit, Zone:Settings:Edit, Zone:SSL and Certificates:Edit, Zone:Zone Rulesets:Edit)"
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "Cloudflare zone ID for job-hunt.dev"
  type        = string
}
