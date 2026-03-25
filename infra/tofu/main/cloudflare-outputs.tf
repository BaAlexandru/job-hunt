output "origin_ca_cert" {
  description = "Cloudflare Origin CA certificate (PEM)"
  value       = cloudflare_origin_ca_certificate.wildcard.certificate
  sensitive   = true
}

output "origin_ca_key" {
  description = "Origin CA private key (PEM)"
  value       = tls_private_key.origin_ca.private_key_pem
  sensitive   = true
}
