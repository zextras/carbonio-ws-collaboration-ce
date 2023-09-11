Kind    = "service-resolver"
Name    = "carbonio-ws-collaboration"

Subsets = {
{{ service "carbonio-ws-collaboration" }}
  {{ .ServiceMeta.ServiceId }} = {
    filter = "Service.Meta.id == \"{{ .ServiceMeta.ServiceId }}\""
  }
{{end}}
}