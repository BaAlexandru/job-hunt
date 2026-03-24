#!/bin/bash
set -euo pipefail

# System updates
apt-get update -y
apt-get upgrade -y

# Install basic tools
apt-get install -y curl jq unzip

# Create 4GB swap file (OOM safety net for m7i-flex.large with 8GB RAM)
fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Set swappiness low -- prefer RAM, use swap only under pressure
echo 'vm.swappiness=10' >> /etc/sysctl.conf
sysctl vm.swappiness=10
