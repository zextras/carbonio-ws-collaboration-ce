consul {
  address = "127.0.0.1:8500"
  retry {
    enabled = true
    attempts = 0
    backoff = "250ms"
    max_backoff = "1m"
  }
}

template {
  source = "templates/carbonio-ws-collaboration-resolver-template.hcl"
  destination = "tmp/carbonio-ws-collaboration-resolver.hcl"
  command = "consul config write tmp/carbonio-ws-collaboration-resolver.hcl"
}

template {
  source = "templates/carbonio-ws-collaboration-router-template.hcl"
  destination = "tmp/carbonio-ws-collaboration-router.hcl"
  command = "consul config write tmp/carbonio-ws-collaboration-router.hcl"
}