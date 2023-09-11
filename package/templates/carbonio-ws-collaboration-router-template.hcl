Kind      = "service-router"
Name      = "carbonio-ws-collaboration"
Routes    = [
{{ range service "carbonio-ws-collaboration" }}
  {
    Match = {
      HTTP = {
        Header = [
          {
            Name    = "x-web-server-id"
            Exact = "{{ .ServiceMeta.ServiceId }}"
          }
        ]
      }
    },
    Destination = {
      Service = "carbonio-ws-collaboration"
      ServiceSubSet = "{{ .ServiceMeta.ServiceId }}"
    }
  },
{{end}}
]