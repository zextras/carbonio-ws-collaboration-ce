services {
  checks = [
    {
      id       = "live"
      http     = "http://127.78.0.4:10000/health/live"
      method   = "GET"
      timeout  = "1s"
      interval = "5s"
    }
  ]

  connect {
    sidecar_service {
      proxy {
        local_service_address = "127.78.0.4"
        upstreams = [
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
            destination_name   = "carbonio-ws-collaboration-db"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20003
          },
          {
            destination_name   = "carbonio-message-dispatcher-http"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20004
          },
          {
            destination_name   = "carbonio-message-broker"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20005
          },
          {
            destination_name   = "carbonio-videoserver"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20006
          },
          {
            destination_name   = "carbonio-videorecorder"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20007
          },
          {
            destination_name   = "carbonio-mailbox-nslookup"
            local_bind_address = "127.78.0.4"
            local_bind_port    = 20008
          }
        ]
      }
    }
  }

  name = "carbonio-ws-collaboration"
  port = 10000
}
