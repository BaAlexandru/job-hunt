terraform {
  backend "s3" {
    bucket       = "jobhunt-tofu-state"
    key          = "infra/main/terraform.tfstate"
    region       = "eu-central-1"
    encrypt      = true
    use_lockfile = true
  }
}
