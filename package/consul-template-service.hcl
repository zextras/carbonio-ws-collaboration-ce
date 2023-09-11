consul {
  address = "localhost:8500"
  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
}

template {
  source = "templates/carbonio-ws-collaboration-template.hcl"
  destination = "tmp/carbonio-ws-collaboration.hcl"
  command = "consul config write tmp/carbonio-ws-collaboration.hcl"
}