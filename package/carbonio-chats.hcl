services {
  checks = [
    {
      id       = "ready",
      http     = "http://127.78.0.4:10000/health/ready",
      method   = "GET",
      timeout  = "1s",
      interval = "5s"
    },
    {
      id       = "live",
      http     = "http://127.78.0.4:10000/health/live",
      method   = "GET",
      timeout  = "1s",
      interval = "5s"
    }
  ],

  connect {
    sidecar_service {
      proxy {
        local_service_address = "127.78.0.4"
        upstreams             = [
          {
            destination_name   = "carbonio-storages"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20000
          },
          {
            destination_name   = "carbonio-user-management"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20001
          },
          {
            destination_name   = "carbonio-preview"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20002
          },
          {
            destination_name   = "carbonio-chats-db"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20003
          },
          {
            destination_name   = "carbonio-chats-messaging-http"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20004
          }
        ]
      }
    }
  }

  name = "carbonio-chats"
  port = 10000
}

watches = [
  {
    type                = "keyprefix"
    prefix              = "carbonio-chats/"
    handler_type        = "http"
    http_handler_config = {
      path            = "http://127.78.0.4:10000/watch/consul/prefix"
      method          = "POST"
      timeout         = "10s"
      tls_skip_verify = false
    }
  }
]